package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.*;
import com.moigferdsrte.gravitychanger.init.ModAttributes;
import com.moigferdsrte.gravitychanger.attributes.DirectionalAttribute;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Passive effect lifetime and temporary rider grant; no navigation decisions. */
public final class MobGravity {
    private static final double RECOVERY_RADIUS=4.0;
    public enum Ownership { NONE, OWNED_EFFECT, BORROWED_RIDER, EXTERNAL }
    public static final class State {
        public Ownership ownership=Ownership.NONE;
        public Direction ownedDirection=Direction.DOWN;
        public Ownership borrowedPreviousOwnership=Ownership.NONE;
        public Direction borrowedPreviousDirection=Direction.DOWN;
        public boolean airUsed;
        public long breadcrumb;
        public java.util.UUID breadcrumbOwner;
        public long retryAt;
        public long visualSequence;
        public void clearBorrow(){borrowedPreviousOwnership=Ownership.NONE;borrowedPreviousDirection=Direction.DOWN;}
    }
    public interface Holder { State clinging$mobGravity(); }
    public static State state(LivingEntity e){return ((Holder)e).clinging$mobGravity();}
    public static boolean supported(LivingEntity e){return e.getAttribute(ModAttributes.GRAVITY_DIRECTION)!=null;}
    public static boolean hasRider(Entity e){return e.getPassengers().stream().anyMatch(p->p instanceof Player && p.isAlive() || hasRider(p));}
    private static boolean hasGrantRider(Entity e){return e.getPassengers().stream().anyMatch(p->p instanceof Player rider && rider.isAlive() && rider.hasEffect(Reorientation.EFFECT) || hasGrantRider(p));}
    public static boolean active(LivingEntity e){return supported(e) && e.isAlive() && (ClingingReoriented.hasEffect(e)||state(e).ownership==Ownership.BORROWED_RIDER&&hasGrantRider(e));}

    private static boolean restore(LivingEntity e,Direction direction){
        var s=state(e);if(e.level().getGameTime()<s.retryAt)return false;
        if(ownedTurn(e,direction,true,null)){s.retryAt=0;return true;}
        var origin=e.position();
        double maxDistanceSqr=RECOVERY_RADIUS*RECOVERY_RADIUS+1.0E-9;
        for(int radius=1;radius<=8;radius++)for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=radius)continue;
            var target=origin.add(x*.5,y*.5,z*.5);
            if(target.distanceToSqr(origin)>maxDistanceSqr)continue;
            if(ownedRelocateTree(e,direction,target,null)){s.retryAt=0;return true;}
        }
        s.retryAt=e.level().getGameTime()+20;return false;
    }
    public static boolean fits(Entity e,AABB box){
        if(!Double.isFinite(box.getSize()) || box.minY<e.level().getMinY() || box.maxY>e.level().getMaxY()+1 || !e.level().getWorldBorder().isWithinBounds(box))return false;
        for(int x=((int)Math.floor(box.minX))>>4;x<=((int)Math.floor(box.maxX))>>4;x++)for(int z=((int)Math.floor(box.minZ))>>4;z<=((int)Math.floor(box.maxZ))>>4;z++)
            if(!e.level().hasChunkAt(new net.minecraft.core.BlockPos(x<<4,(int)box.minY,z<<4)))return false;
        var interior=box.deflate(1e-7);
        if(AnatomyBridge.active(e) && !AnatomyBridge.spaceClear(e,interior))return false;
        return e.level().noCollision(e,interior);
    }
    private static boolean passengersFit(Entity vehicle,Direction direction,Vec3 position){
        for(var passenger:vehicle.getPassengers()){
            var offset=vehicle.getPassengerRidingPosition(passenger).subtract(vehicle.position()).subtract(passenger.getVehicleAttachmentPoint(vehicle));
            var target=position.add(RotationUtil.vecPlayerToWorld(offset,direction));
            var passengerDirection=passenger instanceof Player?direction:GravityDirectionUtil.getGravityDirection(passenger);
            if(!treeFits(passenger,passengerDirection,target))return false;
        }
        return true;
    }
    private static boolean treeFits(Entity root,Direction direction,Vec3 position){
        var box=RotationUtil.makeBoxFromDimensions(root.getDimensions(root.getPose()),direction,position);
        return fits(root,box)&&passengersFit(root,direction,position);
    }
    private static void positionPassengers(Entity vehicle){for(var passenger:vehicle.getPassengers()){vehicle.positionRider(passenger);positionPassengers(passenger);}}
    private static void capturePlayerPassengerGravities(Entity vehicle,java.util.Map<Player,Direction> result){
        for(var passenger:vehicle.getPassengers()){
            if(passenger instanceof Player player)result.put(player,GravityDirectionUtil.getGravityDirection(player));
            capturePlayerPassengerGravities(passenger,result);
        }
    }
    private static void commitTurn(LivingEntity e,Direction direction,Vec3 position,boolean relocate){
        Direction previous=GravityDirectionUtil.getOwnGravityDirection(e);
        var passengerGravityBefore=new java.util.IdentityHashMap<Player,Direction>();
        capturePlayerPassengerGravities(e,passengerGravityBefore);
        if(relocate)e.teleportTo(position.x,position.y,position.z);
        var attribute=e.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        attribute.setBaseValue(DirectionalAttribute.valueOf(direction));
        if(previous!=direction)e.resetFallDistance();
        for(var entry:passengerGravityBefore.entrySet())
            if(GravityDirectionUtil.getGravityDirection(entry.getKey())!=entry.getValue())entry.getKey().resetFallDistance();
        e.setBoundingBox(RotationUtil.makeBoxFromDimensions(e.getDimensions(e.getPose()),direction,position));
        positionPassengers(e);
        e.setOnGround(false);e.verticalCollision=false;e.verticalCollisionBelow=false;e.horizontalCollision=false;
    }
    static boolean relocateTree(LivingEntity e,Direction direction,Vec3 position){
        if(!treeFits(e,direction,position))return false;
        commitTurn(e,direction,position,!e.position().equals(position));
        return true;
    }
    public static boolean turn(LivingEntity e,Direction direction,boolean checkSpace){
        var attribute=e.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if(attribute==null || !attribute.getModifiers().isEmpty())return false;
        if(checkSpace)return relocateTree(e,direction,e.position());
        commitTurn(e,direction,e.position(),false);
        return true;
    }

    private static GravityTransition.Plan ownedPlan(LivingEntity e,Direction direction,GravityTransition.Plan physicalPlan){
        Direction previous=GravityDirectionUtil.getOwnGravityDirection(e);
        if(previous==direction)return null;
        Vec3 heading=GravityTransition.headingFromYaw(previous,e.getYRot());
        if(physicalPlan==null)return GravityTransition.plan(previous,direction,heading);
        if(physicalPlan.previous()!=previous||physicalPlan.target()!=direction)return null;
        return GravityTransition.rebase(physicalPlan,heading);
    }
    private static boolean ownedRelocateTree(LivingEntity e,Direction direction,Vec3 position,GravityTransition.Plan physicalPlan){
        if(!treeFits(e,direction,position))return false;
        Direction previous=GravityDirectionUtil.getOwnGravityDirection(e);
        GravityTransition.Plan transition=ownedPlan(e,direction,physicalPlan);
        if(previous!=direction&&transition==null)return false;
        if(transition!=null){Payloads.visual(e,transition);GravityTransition.applyYawGauge(e,transition.yawDelta());}
        commitTurn(e,direction,position,!e.position().equals(position));
        return true;
    }
    static boolean ownedTurn(LivingEntity e,Direction direction,boolean checkSpace,GravityTransition.Plan physicalPlan){
        var attribute=e.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if(attribute==null || !attribute.getModifiers().isEmpty())return false;
        if(checkSpace)return ownedRelocateTree(e,direction,e.position(),physicalPlan);
        Direction previous=GravityDirectionUtil.getOwnGravityDirection(e);
        GravityTransition.Plan transition=ownedPlan(e,direction,physicalPlan);
        if(previous!=direction&&transition==null)return false;
        if(transition!=null){Payloads.visual(e,transition);GravityTransition.applyYawGauge(e,transition.yawDelta());}
        commitTurn(e,direction,e.position(),false);return true;
    }

    private static void relinquishToExternal(LivingEntity e,Direction direction){
        var s=state(e);s.ownership=Ownership.EXTERNAL;s.ownedDirection=direction;s.clearBorrow();s.retryAt=0;
    }
    public static void externalWrite(LivingEntity e,Direction direction){
        if(e.level().isClientSide()||!supported(e))return;
        relinquishToExternal(e,direction);
    }
    private static boolean ownershipStillMatches(LivingEntity e,State s){
        if(s.ownership!=Ownership.OWNED_EFFECT&&s.ownership!=Ownership.BORROWED_RIDER)return true;
        var attribute=e.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        Direction actual=GravityDirectionUtil.getOwnGravityDirection(e);
        if(attribute==null||!attribute.getModifiers().isEmpty()||actual!=s.ownedDirection){relinquishToExternal(e,actual);return false;}
        return true;
    }
    private static void finishBorrow(State s,Ownership ownership,Direction direction){
        s.ownership=ownership;s.ownedDirection=direction;s.clearBorrow();s.retryAt=0;
    }
    public static boolean borrow(LivingEntity e,Direction direction){return borrow(e,direction,null);}
    public static boolean borrow(LivingEntity e,Direction direction,GravityTransition.Plan physicalPlan){
        if(!supported(e)||!e.isAlive())return false;
        var s=state(e);ownershipStillMatches(e,s);
        Direction current=GravityDirectionUtil.getOwnGravityDirection(e);
        if(current==direction&&s.ownership!=Ownership.BORROWED_RIDER)return true;
        if(s.ownership!=Ownership.BORROWED_RIDER){
            Ownership previous=s.ownership;
            if(previous==Ownership.NONE&&current!=Direction.DOWN)previous=Ownership.EXTERNAL;
            s.borrowedPreviousOwnership=previous;
            s.borrowedPreviousDirection=current;
        }
        if(!ownedTurn(e,direction,true,physicalPlan))return false;
        s.ownership=Ownership.BORROWED_RIDER;s.ownedDirection=direction;s.airUsed=true;s.retryAt=0;
        return true;
    }
    public static void tick(LivingEntity e){
        if(e instanceof Player || e.level().isClientSide() || !supported(e))return;
        var s=state(e);boolean ownEffect=ClingingReoriented.hasEffect(e);
        if(s.airUsed && AirChanges.grounded(e))s.airUsed=false;
        if(!ownershipStillMatches(e,s))return;
        boolean ridden=hasGrantRider(e);
        if(s.ownership==Ownership.BORROWED_RIDER){
            if(ridden)return;
            Ownership previous=s.borrowedPreviousOwnership;Direction previousDirection=s.borrowedPreviousDirection;
            if(previous==Ownership.EXTERNAL){
                if(restore(e,previousDirection))finishBorrow(s,Ownership.EXTERNAL,previousDirection);
                return;
            }
            if(previous==Ownership.OWNED_EFFECT&&ownEffect){
                if(restore(e,previousDirection))finishBorrow(s,Ownership.OWNED_EFFECT,previousDirection);
                return;
            }
            if(ownEffect){
                finishBorrow(s,Ownership.OWNED_EFFECT,GravityDirectionUtil.getOwnGravityDirection(e));
                return;
            }
            if(restore(e,Direction.DOWN))finishBorrow(s,Ownership.NONE,Direction.DOWN);
            return;
        }
        if(s.ownership==Ownership.OWNED_EFFECT&&!ownEffect){
            if(restore(e,Direction.DOWN)){s.ownership=Ownership.NONE;s.ownedDirection=Direction.DOWN;s.retryAt=0;}
        }
    }
    public static boolean transfer(Player player,Entity vehicle,Direction before){
        if(!player.hasEffect(Reorientation.EFFECT) || before==Direction.DOWN || !(vehicle.getRootVehicle() instanceof LivingEntity root) || root instanceof Player)return false;
        Direction previous=GravityDirectionUtil.getOwnGravityDirection(root);
        if(!borrow(root,before))return false;
        if(previous!=before&&player instanceof ServerPlayer serverPlayer){
            var playerState=ClingingReoriented.data(serverPlayer);playerState.visualFrameOwned=true;
            Payloads.visual(serverPlayer,before);Payloads.publish(serverPlayer);
        }
        return true;
    }
    public static boolean replay(LivingEntity pet,Direction direction){
        if(!ClingingReoriented.hasEffect(pet) || !supported(pet) || pet.isPassenger() || pet.isVehicle() || !pet.isAlive())return false;
        var s=state(pet);ownershipStillMatches(pet,s);
        if(AirChanges.grounded(pet))s.airUsed=false;
        Direction current=GravityDirectionUtil.getOwnGravityDirection(pet);
        if(direction==current)return true;
        if((s.ownership==Ownership.EXTERNAL||s.ownership==Ownership.NONE)&&current!=Direction.DOWN)return false;
        if(s.airUsed && !pet.hasEffect(Reorientation.EFFECT))return false;
        boolean airborne=!AirChanges.grounded(pet);
        if(!ownedTurn(pet,direction,true,null))return false;
        s.airUsed|=airborne;s.ownership=Ownership.OWNED_EFFECT;s.ownedDirection=direction;s.clearBorrow();s.retryAt=0;return true;
    }
}
