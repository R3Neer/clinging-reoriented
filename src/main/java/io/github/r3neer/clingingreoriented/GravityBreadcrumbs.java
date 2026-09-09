package io.github.r3neer.clingingreoriented;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GravityBreadcrumbs {
    public static final int LIMIT=64, TTL=1200;
    public record Step(long sequence,ResourceKey<Level> dimension,Vec3 position,Direction direction,long time){}
    private static final Map<UUID,ArrayDeque<Step>> TRAILS=new HashMap<>();
    private static long sequence;
    public static void record(ServerPlayer owner,Direction direction){
        var trail=TRAILS.computeIfAbsent(owner.getUUID(),id->new ArrayDeque<>());
        trail.addLast(new Step(++sequence,owner.level().dimension(),owner.position(),direction,owner.level().getGameTime()));
        while(trail.size()>LIMIT)trail.removeFirst();
    }
    public static void clear(UUID owner){TRAILS.remove(owner);}
    public static void forget(TamableAnimal pet){MobGravity.state(pet).breadcrumb=sequence;}
    public static void clearAll(){TRAILS.clear();}
    public static void prune(net.minecraft.server.MinecraftServer server){
        TRAILS.entrySet().removeIf(entry->{var owner=server.getPlayerList().getPlayer(entry.getKey());if(owner==null || !owner.isAlive())return true;
            entry.getValue().removeIf(step->step.dimension()!=owner.level().dimension() || owner.level().getGameTime()-step.time()>TTL);return entry.getValue().isEmpty();});
    }
    /** Called only by the running vanilla FollowOwnerGoal, never by a proximity scan. */
    public static void follow(TamableAnimal pet){
        if(pet.level().isClientSide() || !pet.isTame() || pet.unableToMoveToOwner() || !(pet.getOwner() instanceof ServerPlayer owner)
            || !owner.isAlive() || owner.level()!=pet.level() || !pet.isAlive() || !ClingingReoriented.hasEffect(pet))return;
        var trail=TRAILS.get(owner.getUUID());if(trail==null)return;
        var state=MobGravity.state(pet);
        if(state.breadcrumbOwner!=null && !state.breadcrumbOwner.equals(owner.getUUID()))state.breadcrumb=sequence;
        state.breadcrumbOwner=owner.getUUID();
        for(var step:trail){
            if(step.sequence()<=state.breadcrumb)continue;
            if(step.dimension()!=pet.level().dimension() || pet.level().getGameTime()-step.time()>TTL){state.breadcrumb=step.sequence();continue;}
            if(pet.position().distanceToSqr(step.position())>Math.pow(Math.clamp(pet.getBbWidth(),.5,1.5),2))return;
            if(MobGravity.replay(pet,step.direction()))state.breadcrumb=step.sequence();
            return; // No cascading several turns at one point in one tick.
        }
    }
}
