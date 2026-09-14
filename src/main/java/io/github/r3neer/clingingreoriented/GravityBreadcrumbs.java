package io.github.r3neer.clingingreoriented;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GravityBreadcrumbs {
    public static final int LIMIT=64, TTL=1200;
    static final int PATH_RETRY_TICKS=10;
    public record Step(long sequence,ResourceKey<Level> dimension,Vec3 position,Direction direction,long time){}
    private static final Map<UUID,ArrayDeque<Step>> TRAILS=new HashMap<>();
    private static long sequence;
    public static void record(ServerPlayer owner,Direction direction){
        var trail=TRAILS.computeIfAbsent(owner.getUUID(),id->new ArrayDeque<>());
        trail.addLast(new Step(++sequence,owner.level().dimension(),owner.position(),direction,owner.level().getGameTime()));
        while(trail.size()>LIMIT)trail.removeFirst();
    }
    public static void clear(UUID owner){TRAILS.remove(owner);}
    public static void forget(TamableAnimal pet){MobGravity.state(pet).breadcrumb=sequence;releaseRoute(pet);}
    public static void clearAll(){TRAILS.clear();}
    public static void prune(net.minecraft.server.MinecraftServer server){
        TRAILS.entrySet().removeIf(entry->{var owner=server.getPlayerList().getPlayer(entry.getKey());if(owner==null || !owner.isAlive())return true;
            entry.getValue().removeIf(step->step.dimension()!=owner.level().dimension() || owner.level().getGameTime()-step.time()>TTL);return entry.getValue().isEmpty();});
    }
    private static Step pending(TamableAnimal pet){
        if(pet.level().isClientSide() || !pet.isTame() || pet.unableToMoveToOwner() || !(pet.getOwner() instanceof ServerPlayer owner)
            || !owner.isAlive() || owner.level()!=pet.level() || !MobGravity.canReplayBreadcrumb(pet)){releaseRoute(pet);return null;}
        var trail=TRAILS.get(owner.getUUID());if(trail==null){releaseRoute(pet);return null;}
        var state=MobGravity.state(pet);
        if(state.breadcrumbOwner!=null && !state.breadcrumbOwner.equals(owner.getUUID()))state.breadcrumb=sequence;
        state.breadcrumbOwner=owner.getUUID();
        for(var step:trail){
            if(step.sequence()<=state.breadcrumb)continue;
            if(step.dimension()!=pet.level().dimension() || pet.level().getGameTime()-step.time()>TTL){state.breadcrumb=step.sequence();continue;}
            return step;
        }
        releaseRoute(pet);return null;
    }
    public static boolean hasPending(TamableAnimal pet){return pending(pet)!=null;}
    public static boolean canPursue(TamableAnimal pet){return pending(pet)!=null&&AirChanges.grounded(pet);}
    static Vec3 projectedTarget(Vec3 pet,Vec3 breadcrumb,Direction gravity){
        return switch(gravity.getAxis()){
            case X -> new Vec3(pet.x,breadcrumb.y,breadcrumb.z);
            case Y -> new Vec3(breadcrumb.x,pet.y,breadcrumb.z);
            case Z -> new Vec3(breadcrumb.x,breadcrumb.y,pet.z);
        };
    }
    static double movementPlaneDistanceSqr(Vec3 pet,Vec3 breadcrumb,Direction gravity){
        return pet.distanceToSqr(projectedTarget(pet,breadcrumb,gravity));
    }
    private static void releaseRoute(TamableAnimal pet){
        var state=MobGravity.state(pet);
        if(state.breadcrumbRouteOwned)pet.getNavigation().stop();
        clearRouteState(state);
    }
    private static void clearRouteState(MobGravity.State state){
        state.breadcrumbRoute=-1;state.breadcrumbRouteRetryAt=0;state.breadcrumbRouteOwned=false;state.breadcrumbNavigation=null;
    }
    /** Vanilla already stopped this goal's path; only relinquish our causal marker. */
    public static void goalStopped(TamableAnimal pet){clearRouteState(MobGravity.state(pet));}
    /** Owns FollowOwnerGoal only while pursuing the oldest valid gravity breadcrumb. */
    public static boolean follow(TamableAnimal pet,double speed,float stopDistance){
        var step=pending(pet);if(step==null)return false;
        if(!AirChanges.grounded(pet)){releaseRoute(pet);return true;}
        var state=MobGravity.state(pet);var navigation=pet.getNavigation();long time=pet.level().getGameTime();
        if(state.breadcrumbRoute!=step.sequence()||state.breadcrumbNavigation!=navigation){
            releaseRoute(pet);state.breadcrumbRoute=step.sequence();state.breadcrumbRouteOwned=true;state.breadcrumbRouteRetryAt=0;
            state.breadcrumbNavigation=navigation;
        }
        Direction gravity=GravityDirectionUtil.getGravityDirection(pet);
        double radius=Math.clamp(Math.max(pet.getBbWidth(),stopDistance),.5,2);
        if(movementPlaneDistanceSqr(pet.position(),step.position(),gravity)<=radius*radius){
            navigation.stop();
            if(MobGravity.replay(pet,step.direction())){
                state.breadcrumb=step.sequence();clearRouteState(state);
            }
            return true;
        }
        if(time>=state.breadcrumbRouteRetryAt){
            var target=projectedTarget(pet.position(),step.position(),gravity);
            navigation.moveTo(target.x,target.y,target.z,speed);
            state.breadcrumbRouteRetryAt=time+PATH_RETRY_TICKS;
        }
        return true;
    }
}
