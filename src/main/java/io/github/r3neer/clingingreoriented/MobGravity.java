package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.*;
import com.moigferdsrte.gravitychanger.init.ModAttributes;
import com.moigferdsrte.gravitychanger.attributes.DirectionalAttribute;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Passive effect lifetime and temporary rider grant; no navigation decisions. */
public final class MobGravity {
    public static final class State {
        public boolean effectSeen, borrowed, airUsed;
        public long breadcrumb;
        public java.util.UUID breadcrumbOwner;
        public long retryAt;
    }
    public interface Holder { State clinging$mobGravity(); }
    public static State state(LivingEntity e){return ((Holder)e).clinging$mobGravity();}
    public static boolean supported(LivingEntity e){return e.getAttribute(ModAttributes.GRAVITY_DIRECTION)!=null;}
    public static boolean hasRider(Entity e){return e.getPassengers().stream().anyMatch(p->p instanceof Player && p.isAlive() || hasRider(p));}
    private static boolean hasGrantRider(Entity e){return e.getPassengers().stream().anyMatch(p->p instanceof Player rider && rider.isAlive() && rider.hasEffect(Reorientation.EFFECT) || hasGrantRider(p));}
    public static boolean active(LivingEntity e){return supported(e) && e.isAlive() && (ClingingReoriented.hasEffect(e)||state(e).borrowed&&hasGrantRider(e));}
    private static boolean restore(LivingEntity e){
        var s=state(e);if(e.level().getGameTime()<s.retryAt)return false;
        if(turn(e,Direction.DOWN,true))return true;
        var origin=e.position();
        for(int radius=1;radius<=8;radius++)for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=radius)continue;
            var target=origin.add(x*.5,y*.5,z*.5);
            var box=RotationUtil.makeBoxFromDimensions(e.getDimensions(e.getPose()),Direction.DOWN,target);
            if(fits(e,box)){e.teleportTo(target.x,target.y,target.z);return turn(e,Direction.DOWN,true);}
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
    private static boolean passengersFit(Entity vehicle,Direction direction,net.minecraft.world.phys.Vec3 position){
        for(var passenger:vehicle.getPassengers()){
            // Audited vanilla attachment getters, with the same cardinal rotation
            // used by Gravity Changer's positionRider hook. No gravity mutation.
            var offset=vehicle.getPassengerRidingPosition(passenger).subtract(vehicle.position()).subtract(passenger.getVehicleAttachmentPoint(vehicle));
            var target=position.add(RotationUtil.vecPlayerToWorld(offset,direction));
            var passengerDirection=passenger instanceof Player?direction:GravityDirectionUtil.getGravityDirection(passenger);
            if(!fits(passenger,RotationUtil.makeBoxFromDimensions(passenger.getDimensions(passenger.getPose()),passengerDirection,target))
                || !passengersFit(passenger,passengerDirection,target))return false;
        }
        return true;
    }
    private static void positionPassengers(Entity vehicle){for(var passenger:vehicle.getPassengers()){vehicle.positionRider(passenger);positionPassengers(passenger);}}
    public static boolean turn(LivingEntity e,Direction direction,boolean checkSpace){
        var attribute=e.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if(attribute==null || !attribute.getModifiers().isEmpty())return false;
        var box=RotationUtil.makeBoxFromDimensions(e.getDimensions(e.getPose()),direction,e.position());
        if(checkSpace && (!fits(e,box) || !passengersFit(e,direction,e.position())))return false;
        // The public setter recentres mobs by teleporting. Use its same audited
        // syncable base attribute here so the validated pivot and world velocity stay fixed.
        attribute.setBaseValue(DirectionalAttribute.valueOf(direction));e.setBoundingBox(box);
        positionPassengers(e);
        e.setOnGround(false);e.verticalCollision=false;e.verticalCollisionBelow=false;e.horizontalCollision=false;
        return true;
    }
    public static void tick(LivingEntity e){
        if(e instanceof Player || e.level().isClientSide() || !supported(e))return;
        var s=state(e);boolean own=ClingingReoriented.hasEffect(e);
        if(s.airUsed && AirChanges.grounded(e))s.airUsed=false;
        if(own)s.effectSeen=true;
        boolean ridden=hasGrantRider(e);
        if(!own && !ridden && (s.effectSeen || s.borrowed)) {
            if(restore(e)){s.effectSeen=false;s.borrowed=false;}
        }
        if(!ridden && own)s.borrowed=false;
    }
    public static boolean transfer(Player player,Entity vehicle,Direction before){
        if(!player.hasEffect(Reorientation.EFFECT) || before==Direction.DOWN || !(vehicle.getRootVehicle() instanceof LivingEntity root) || root instanceof Player)return false;
        if(!turn(root,before,true))return false;
        state(root).borrowed=true;state(root).effectSeen|=ClingingReoriented.hasEffect(root);
        return true;
    }
    public static boolean replay(LivingEntity pet,Direction direction){
        if(!ClingingReoriented.hasEffect(pet) || !supported(pet) || pet.isPassenger() || pet.isVehicle() || !pet.isAlive())return false;
        var s=state(pet);if(AirChanges.grounded(pet))s.airUsed=false;
        if(direction==GravityDirectionUtil.getGravityDirection(pet))return true;
        if(s.airUsed && !pet.hasEffect(Reorientation.EFFECT))return false;
        boolean airborne=!AirChanges.grounded(pet);
        if(!turn(pet,direction,true))return false;
        s.airUsed|=airborne;s.effectSeen=true;return true;
    }
}
