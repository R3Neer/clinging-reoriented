package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative safety net for sustained Clinging flight/fall.
 *
 * <p>Two independent failures are fenced here:
 * <ul>
 *   <li>world velocity is capped to the vanilla-like falling terminal speed so repeated
 *       gravity turns cannot accumulate an arbitrarily large world vector;</li>
 *   <li>a player may not advance beyond hard world bounds or into a chunk column that is not
 *       currently available to the server. Chunk-frontier momentum is retained and released as
 *       soon as the next step is ready; hard-boundary momentum is discarded.</li>
 * </ul>
 *
 * <p>This is deliberately server-side. Rendering can run at any frame rate, but position/chunk
 * authority exists at 20 TPS and must not be implemented as a client-frame teleport loop.
 */
public final class FlightSafety {
    /** Vanilla falling converges to roughly 3.92 blocks/tick. */
    public static final double TERMINAL_SPEED=3.92D;
    private static final double TERMINAL_SPEED_SQR=TERMINAL_SPEED*TERMINAL_SPEED;
    private static final int FRONTIER_BISECTIONS=16;

    private FlightSafety() {}

    /** Snapshot the last unquestionably valid position before entity movement for this server tick. */
    public static void capture(ServerPlayer player){
        PlayerData state=ClingingReoriented.data(player);
        if(!eligible(player)||!ClingingReoriented.controlsPhysics(player))return;
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        if(readyAt(player,player.position(),gravity))state.flightSafePosition=player.position();
    }

    public static void tick(ServerPlayer player){
        PlayerData state=ClingingReoriented.data(player);
        if(!eligible(player)||!ClingingReoriented.controlsPhysics(player)){
            state.clearFlightSafety();
            return;
        }

        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        Vec3 current=player.position();

        // If a previous tick stopped at an unloaded frontier, keep the entity stationary while
        // testing the retained one-tick step. The surrounding player ticket can then finish
        // loading the adjacent chunk without us repeatedly overshooting and teleporting back.
        if(state.flightSafetyHolding){
            Vec3 held=clampVelocity(state.flightHeldVelocity);
            Vec3 probe=current.add(held);
            if(!withinHardBounds(player,probe,gravity)){
                state.clearFlightSafetyHold();
                player.setDeltaMovement(Vec3.ZERO);
                ImpactState.clear(player);
            }else if(chunksReady(player,probe,gravity)){
                state.clearFlightSafetyHold();
                player.setDeltaMovement(held);
                state.flightSafePosition=current;
            }else{
                player.setDeltaMovement(Vec3.ZERO);
                ImpactState.clear(player);
                return;
            }
        }

        Vec3 velocity=clampVelocity(player.getDeltaMovement());
        if(!velocity.equals(player.getDeltaMovement()))player.setDeltaMovement(velocity);

        current=player.position();
        if(readyAt(player,current,gravity)){
            state.flightSafePosition=current;
            return;
        }

        boolean hardBreach=!withinHardBounds(player,current,gravity);
        Vec3 safe=state.flightSafePosition;
        if(safe==null||!readyAt(player,safe,gravity)){
            // START_SERVER_TICK normally guarantees a safe anchor. The fallback covers activation
            // or lifecycle races: first try the previous one-tick location, then project a hard
            // out-of-world position back inside the legal border/build volume.
            Vec3 previous=current.subtract(velocity);
            if(readyAt(player,previous,gravity))safe=previous;
            else if(hardBreach){
                Vec3 clamped=insideHardBounds(player,current,gravity);
                teleport(player,clamped,Vec3.ZERO);
                state.flightSafePosition=clamped;
                state.clearFlightSafetyHold();
                ImpactState.clear(player);
                return;
            }else{
                // We are already in an unavailable chunk with no prior sample. Hold this position;
                // the player's own chunk ticket can finish loading it, after which capture/tick
                // will establish a normal anchor again.
                state.flightSafePosition=current;
                state.flightSafetyHolding=true;
                state.flightHeldVelocity=velocity;
                player.setDeltaMovement(Vec3.ZERO);
                ImpactState.clear(player);
                return;
            }
        }

        Predicate<Vec3> accepted=hardBreach
            ? pos->withinHardBounds(player,pos,gravity)
            : pos->readyAt(player,pos,gravity);
        Vec3 frontier=furthestReady(safe,current,accepted);
        teleport(player,frontier,Vec3.ZERO);
        state.flightSafePosition=frontier;
        // A safety correction is not a physical collision. Erase any armed impact/fall sample so
        // the rescue itself can never become lethal on the following tick.
        ImpactState.clear(player);

        if(hardBreach){
            // There is nothing to load beyond a hard border/build limit. Do not keep a latent
            // outward vector that would hammer the same boundary forever.
            state.clearFlightSafetyHold();
            player.setDeltaMovement(Vec3.ZERO);
        }else{
            // Unloaded chunk frontier: preserve the capped momentum, but hold position until the
            // next one-tick step is actually backed by available chunks.
            state.flightSafetyHolding=true;
            state.flightHeldVelocity=velocity;
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    static Vec3 clampVelocity(Vec3 velocity){
        if(velocity==null||!Double.isFinite(velocity.x+velocity.y+velocity.z))return Vec3.ZERO;
        double speedSqr=velocity.lengthSqr();
        if(speedSqr<=TERMINAL_SPEED_SQR)return velocity;
        return velocity.scale(TERMINAL_SPEED/Math.sqrt(speedSqr));
    }

    static Vec3 furthestReady(Vec3 safe,Vec3 target,Predicate<Vec3> ready){
        if(safe==null||target==null||ready==null)throw new IllegalArgumentException("safe/target/ready required");
        if(!ready.test(safe))return safe;
        if(ready.test(target))return target;
        double low=0.0D,high=1.0D;
        Vec3 delta=target.subtract(safe);
        for(int i=0;i<FRONTIER_BISECTIONS;i++){
            double mid=(low+high)*0.5D;
            Vec3 candidate=safe.add(delta.scale(mid));
            if(ready.test(candidate))low=mid;else high=mid;
        }
        return safe.add(delta.scale(low));
    }

    private static boolean readyAt(ServerPlayer player,Vec3 position,Direction gravity){
        return withinHardBounds(player,position,gravity)&&chunksReady(player,position,gravity);
    }

    private static boolean withinHardBounds(ServerPlayer player,Vec3 position,Direction gravity){
        AABB box=boxAt(player,position,gravity);
        return box.minY>=player.level().getMinY()
            && box.maxY<=player.level().getMaxY()+1
            && player.level().getWorldBorder().isWithinBounds(box);
    }

    private static Vec3 insideHardBounds(ServerPlayer player,Vec3 position,Direction gravity){
        AABB box=boxAt(player,position,gravity);
        var border=player.level().getWorldBorder();
        double dx=0.0D,dy=0.0D,dz=0.0D;
        double epsilon=1.0E-4D;
        if(box.minX<border.getMinX()+epsilon)dx=(border.getMinX()+epsilon)-box.minX;
        else if(box.maxX>border.getMaxX()-epsilon)dx=(border.getMaxX()-epsilon)-box.maxX;
        if(box.minZ<border.getMinZ()+epsilon)dz=(border.getMinZ()+epsilon)-box.minZ;
        else if(box.maxZ>border.getMaxZ()-epsilon)dz=(border.getMaxZ()-epsilon)-box.maxZ;
        double minY=player.level().getMinY()+epsilon,maxY=player.level().getMaxY()+1.0D-epsilon;
        if(box.minY<minY)dy=minY-box.minY;
        else if(box.maxY>maxY)dy=maxY-box.maxY;
        return position.add(dx,dy,dz);
    }

    private static boolean chunksReady(ServerPlayer player,Vec3 position,Direction gravity){
        AABB box=boxAt(player,position,gravity);
        int y=(int)Math.floor(Math.max(player.level().getMinY(),Math.min(player.level().getMaxY(),box.minY)));
        int minChunkX=((int)Math.floor(box.minX))>>4;
        int maxChunkX=((int)Math.floor(box.maxX))>>4;
        int minChunkZ=((int)Math.floor(box.minZ))>>4;
        int maxChunkZ=((int)Math.floor(box.maxZ))>>4;
        for(int x=minChunkX;x<=maxChunkX;x++)for(int z=minChunkZ;z<=maxChunkZ;z++)
            if(!player.level().hasChunkAt(new BlockPos(x<<4,y,z<<4)))return false;
        return true;
    }

    private static AABB boxAt(ServerPlayer player,Vec3 position,Direction gravity){
        return RotationUtil.makeBoxFromDimensions(player.getDimensions(player.getPose()),gravity,position);
    }

    private static boolean eligible(ServerPlayer player){
        return player.isAlive()&&!player.isSpectator()&&!player.isSleeping()&&!player.isPassenger()
            &&!player.isFallFlying()&&!player.isInWater()&&!player.isInLava()&&!player.getAbilities().flying
            &&!AirChanges.grounded(player);
    }

    private static void teleport(ServerPlayer player,Vec3 position,Vec3 velocity){
        player.connection.teleport(new PositionMoveRotation(position,velocity,player.getYRot(),player.getXRot()),Set.<Relative>of());
    }
}
