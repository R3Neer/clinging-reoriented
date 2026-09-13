package io.github.r3neer.clingingreoriented;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Per-living-entity handoff from movement collision to the impact pipeline. */
public final class ImpactState {
    public static final class State {
        public boolean armed;
        public boolean moveActive;
        public long moveSequence;
        public long handledSequence=-1L;
        public Vec3 start=Vec3.ZERO;
        public Vec3 intended=Vec3.ZERO;
        public void clear(){armed=false;moveActive=false;handledSequence=-1L;start=Vec3.ZERO;intended=Vec3.ZERO;}
    }
    public interface Holder { State clinging$impactState(); }
    private ImpactState() {}
    public static State state(LivingEntity entity){return ((Holder)entity).clinging$impactState();}

    public static boolean owns(LivingEntity entity){
        if(entity instanceof Player player)return ClingingReoriented.controlsPhysics(player);
        var ownership=MobGravity.state(entity).ownership;
        return entity.isAlive()&&(ownership==MobGravity.Ownership.OWNED_EFFECT||ownership==MobGravity.Ownership.BORROWED_RIDER);
    }

    public static void beginMove(LivingEntity entity,Vec3 intended){
        var state=state(entity);
        if(entity.isFallFlying()||entity.isInWater()||entity.isInLava()||!entity.isAlive()){
            state.clear();entity.fallDistance=0.0F;return;
        }
        if(owns(entity))state.armed=true;
        if(!state.armed)return;
        state.moveActive=true;state.moveSequence++;state.start=entity.position();state.intended=ImpactPhysics.finite(intended)?intended:Vec3.ZERO;
    }

    public static void endMove(LivingEntity entity){
        var state=state(entity);state.moveActive=false;
        if(state.armed&&!owns(entity)&&entity.onGround())state.clear();
    }

    public static void clear(LivingEntity entity){state(entity).clear();entity.fallDistance=0.0F;}
}
