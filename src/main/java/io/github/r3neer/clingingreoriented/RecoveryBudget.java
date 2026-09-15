package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Incremental version of the legacy 4-block recovery scan.
 *
 * <p>The candidate order is intentionally identical to the old radius/y/x/z loops. The only
 * behavioural change is scheduling: at most {@link #CANDIDATES_PER_CALL} relocation candidates are
 * tested by one entity in one invocation, so a blocked recovery cannot monopolize a server tick.
 */
public final class RecoveryBudget {
    static final int CANDIDATES_PER_CALL=64;
    private static final double RADIUS=4.0D;
    private static final double MAX_DISTANCE_SQR=RADIUS*RADIUS+1.0E-9D;
    static record Offset(int x,int y,int z) {}
    private static final List<Offset> OFFSETS=buildOffsets();

    private static final class Cursor {
        Direction direction;
        int index;
        boolean completedCycle;
        Cursor(Direction direction){this.direction=direction;}
    }
    private static final Map<ServerPlayer,Cursor> PLAYERS=new WeakHashMap<>();
    private static final Map<LivingEntity,Cursor> MOBS=new WeakHashMap<>();

    private static volatile MethodHandle playerWriteTransition;
    private static volatile MethodHandle mobOwnedRelocateTree;

    private RecoveryBudget() {}

    static List<Offset> offsetsForTest(){return OFFSETS;}

    private static List<Offset> buildOffsets(){
        ArrayList<Offset> result=new ArrayList<>();
        for(int radius=1;radius<=8;radius++)for(int y=-radius;y<=radius;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=radius)continue;
            double dx=x*.5D,dy=y*.5D,dz=z*.5D;
            if(dx*dx+dy*dy+dz*dz<=MAX_DISTANCE_SQR)result.add(new Offset(x,y,z));
        }
        return List.copyOf(result);
    }

    public static boolean retirePlayer(ServerPlayer player){
        Direction previous=GravityDirectionUtil.getGravityDirection(player);
        if(previous==Direction.DOWN){PLAYERS.remove(player);return true;}

        Vec3 heading=GravityTransition.headingFromYaw(previous,player.getYRot());
        GravityTransition.Plan transition=GravityTransition.plan(previous,Direction.DOWN,heading);
        Vec3 origin=player.position();
        var dimensions=player.getDimensions(player.getPose());

        if(ClingingReoriented.fits(player,RotationUtil.makeBoxFromDimensions(dimensions,Direction.DOWN,origin),null)){
            completePlayer(player,origin,transition);return true;
        }

        Cursor cursor=PLAYERS.computeIfAbsent(player,ignored->new Cursor(previous));
        if(cursor.direction!=previous){cursor.direction=previous;cursor.index=0;cursor.completedCycle=false;}
        cursor.completedCycle=false;
        int tested=0;
        while(cursor.index<OFFSETS.size()&&tested<CANDIDATES_PER_CALL){
            Offset offset=OFFSETS.get(cursor.index++);tested++;
            Vec3 target=origin.add(offset.x*.5D,offset.y*.5D,offset.z*.5D);
            if(ClingingReoriented.fits(player,RotationUtil.makeBoxFromDimensions(dimensions,Direction.DOWN,target),null)){
                completePlayer(player,target,transition);return true;
            }
        }

        var state=ClingingReoriented.data(player);
        state.retirementPending=true;
        if(cursor.index>=OFFSETS.size()){
            cursor.index=0;cursor.completedCycle=true;
            state.nextRetirementAttempt=player.level().getGameTime()+20L;
            org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID).warn(
                "Cannot retire Clinging for {} yet: no collision-free DOWN placement within {} blocks",player.getUUID(),RADIUS);
        }else{
            // A direct caller such as anchor release must not allow END_SERVER_TICK to consume a
            // second batch in the same tick. Reconcile will continue this cursor on the next tick.
            state.nextRetirementAttempt=player.level().getGameTime()+1L;
        }
        return false;
    }

    /** Let reconcile revisit a partial scan next tick; a completed full cycle keeps the legacy 20-tick pause. */
    public static boolean playerNeedsNextTick(ServerPlayer player){
        Cursor cursor=PLAYERS.get(player);
        if(cursor==null)return false;
        if(cursor.completedCycle){cursor.completedCycle=false;return false;}
        return true;
    }

    public static void clearPlayer(ServerPlayer player){PLAYERS.remove(player);}

    public static boolean restoreMob(LivingEntity entity,Direction direction){
        var state=MobGravity.state(entity);long now=entity.level().getGameTime();
        if(now<state.retryAt)return false;
        if(MobGravity.ownedTurn(entity,direction,true,null)){
            state.retryAt=0;MOBS.remove(entity);return true;
        }

        Cursor cursor=MOBS.computeIfAbsent(entity,ignored->new Cursor(direction));
        if(cursor.direction!=direction){cursor.direction=direction;cursor.index=0;cursor.completedCycle=false;}
        Vec3 origin=entity.position();int tested=0;
        while(cursor.index<OFFSETS.size()&&tested<CANDIDATES_PER_CALL){
            Offset offset=OFFSETS.get(cursor.index++);tested++;
            Vec3 target=origin.add(offset.x*.5D,offset.y*.5D,offset.z*.5D);
            if(mobOwnedRelocateTree(entity,direction,target)){
                state.retryAt=0;MOBS.remove(entity);return true;
            }
        }
        if(cursor.index>=OFFSETS.size()){
            cursor.index=0;cursor.completedCycle=true;state.retryAt=now+20;
        }
        return false;
    }

    public static void clearMob(LivingEntity entity){MOBS.remove(entity);}

    private static void completePlayer(ServerPlayer player,Vec3 position,GravityTransition.Plan transition){
        Payloads.visual(player,transition);
        invokePlayerWriteTransition(player,Direction.DOWN,position,transition);
        PLAYERS.remove(player);
    }

    private static void invokePlayerWriteTransition(ServerPlayer player,Direction direction,Vec3 position,GravityTransition.Plan transition){
        try{playerWriteTransition().invoke(player,direction,position,transition);}
        catch(Throwable failure){throw new IllegalStateException("Cannot complete budgeted player gravity recovery",failure);}
    }

    private static boolean mobOwnedRelocateTree(LivingEntity entity,Direction direction,Vec3 position){
        try{return (boolean)mobOwnedRelocateTree().invoke(entity,direction,position,null);}
        catch(Throwable failure){throw new IllegalStateException("Cannot complete budgeted mob gravity recovery",failure);}
    }

    private static MethodHandle playerWriteTransition() throws ReflectiveOperationException{
        MethodHandle handle=playerWriteTransition;if(handle!=null)return handle;
        synchronized(RecoveryBudget.class){
            handle=playerWriteTransition;if(handle==null){
                MethodHandles.Lookup lookup=MethodHandles.privateLookupIn(ClingingReoriented.class,MethodHandles.lookup());
                handle=lookup.findStatic(ClingingReoriented.class,"writeTransition",
                    MethodType.methodType(void.class,ServerPlayer.class,Direction.class,Vec3.class,GravityTransition.Plan.class));
                playerWriteTransition=handle;
            }
        }
        return handle;
    }

    private static MethodHandle mobOwnedRelocateTree() throws ReflectiveOperationException{
        MethodHandle handle=mobOwnedRelocateTree;if(handle!=null)return handle;
        synchronized(RecoveryBudget.class){
            handle=mobOwnedRelocateTree;if(handle==null){
                MethodHandles.Lookup lookup=MethodHandles.privateLookupIn(MobGravity.class,MethodHandles.lookup());
                handle=lookup.findStatic(MobGravity.class,"ownedRelocateTree",
                    MethodType.methodType(boolean.class,LivingEntity.class,Direction.class,Vec3.class,GravityTransition.Plan.class));
                mobOwnedRelocateTree=handle;
            }
        }
        return handle;
    }
}
