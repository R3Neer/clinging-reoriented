package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.entity.ai.DirectionalGroundNodeEvaluator;
import com.moigferdsrte.gravitychanger.entity.ai.DirectionalGroundPathNavigation;
import com.moigferdsrte.gravitychanger.entity.ai.DirectionalMobAiUtil;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * S05-B bounded strategic planner: one tactical surface path, then at most five physical gravity forecasts.
 * It never queries the mob's live navigation with createPath, because that method mutates navigation metadata.
 */
public final class MobGravityLocalPlanner {
    private static final double WALK_COST_PER_BLOCK=1.0D;

    public enum Kind { WALK, TRANSITION, NO_PLAN }

    /** Goal semantics are supplied by follow/chase/flee layers in S06. */
    public interface Goal {
        Vec3 focus();
        boolean satisfied(Vec3 position,Direction gravity);
        double heuristic(Vec3 position,Direction gravity);
    }

    public record Plan(
        Kind kind,
        Path walkPath,
        Vec3 frontier,
        Direction terminalGravity,
        MobGravityPlanner.Transition transition,
        double walkingCost,
        double heuristicCost,
        double totalCost,
        int transitionEvaluations
    ) {
        public boolean usable(){return kind!=Kind.NO_PLAN&&Double.isFinite(totalCost);}
    }

    private MobGravityLocalPlanner() {}

    public static Plan plan(Mob mob,Goal goal){
        return plan(mob,goal,MobGravityPlanner.DEFAULT_TRANSITION_HORIZON_TICKS);
    }

    public static Plan plan(Mob mob,Goal goal,int transitionHorizonTicks){
        if(mob==null||goal==null||!mob.isAlive())return noPlan(mob,null,null,0.0D,0);
        Vec3 focus=safeFocus(goal);
        if(!finite(focus))return noPlan(mob,null,mob.position(),0.0D,0);

        Direction current=GravityDirectionUtil.getOwnGravityDirection(mob);
        Vec3 currentPosition=mob.position();
        if(safeSatisfied(goal,currentPosition,current))
            return new Plan(Kind.WALK,null,currentPosition,current,null,0.0D,0.0D,0.0D,0);

        GroundPathNavigation mirror=mirrorNavigation(mob,current);
        if(mirror==null)return noPlan(mob,null,currentPosition,0.0D,0);

        Vec3 projected=DirectionalMobAiUtil.projectOntoMovementPlane(currentPosition,focus,current);
        var targetNode=DirectionalGroundNodeEvaluator.nodePosition(projected,current);
        Path path=mirror.createPath(targetNode,1);
        Vec3 frontier=currentPosition;
        if(path!=null&&path.getEndNode()!=null)
            frontier=DirectionalGroundNodeEvaluator.entityPosition(path.getEndNode().asBlockPos(),current);
        double walkingCost=pathCost(currentPosition,path,current)*WALK_COST_PER_BLOCK;
        if(!Double.isFinite(walkingCost))return noPlan(mob,path,frontier,Double.POSITIVE_INFINITY,0);

        if(path!=null&&path.canReach()&&safeSatisfied(goal,frontier,current))
            return new Plan(Kind.WALK,path,frontier,current,null,walkingCost,0.0D,walkingCost,0);

        MobGravityPlanner.Transition best=null;
        Direction bestGravity=current;
        double bestHeuristic=Double.POSITIVE_INFINITY;
        double bestTotal=Double.POSITIVE_INFINITY;
        int evaluations=0;
        for(Direction candidate:Direction.values()){
            if(candidate==current)continue;
            evaluations++;
            var evaluation=MobGravityPlanner.evaluateGroundedLaunch(mob,frontier,candidate,transitionHorizonTicks);
            if(!evaluation.accepted())continue;
            var transition=evaluation.transition();
            Vec3 landingPosition=RotationUtil.getCenterAlignedPosition(
                transition.landingBody(),mob.getDimensions(mob.getPose()),candidate);
            double heuristic=safeHeuristic(goal,landingPosition,candidate);
            if(!Double.isFinite(heuristic))continue;
            double total=walkingCost+transition.physicalCost()+heuristic;
            if(!Double.isFinite(total))continue;
            if(total<bestTotal){
                best=transition;bestGravity=candidate;bestHeuristic=heuristic;bestTotal=total;
            }
        }

        if(best==null)return noPlan(mob,path,frontier,walkingCost,evaluations);
        return new Plan(Kind.TRANSITION,path,frontier,bestGravity,best,walkingCost,bestHeuristic,bestTotal,evaluations);
    }

    /** New mirror, same mob/world and movement capabilities; mutations stay inside this throwaway navigation. */
    private static GroundPathNavigation mirrorNavigation(Mob mob,Direction gravity){
        if(!(mob.getNavigation() instanceof GroundPathNavigation live))return null;
        GroundPathNavigation mirror=gravity==Direction.DOWN
            ?new GroundPathNavigation(mob,mob.level())
            :new DirectionalGroundPathNavigation(mob,mob.level(),gravity);
        NodeEvaluator source=live.getNodeEvaluator();
        if(source!=null){
            mirror.setCanFloat(source.canFloat());
            mirror.setCanOpenDoors(source.canOpenDoors());
            mirror.getNodeEvaluator().setCanPassDoors(source.canPassDoors());
            mirror.setCanWalkOverFences(source.canWalkOverFences());
        }
        return mirror;
    }

    private static double pathCost(Vec3 start,Path path,Direction gravity){
        if(path==null||path.getNodeCount()==0)return 0.0D;
        Vec3 previous=start;double result=0.0D;
        for(int i=0;i<path.getNodeCount();i++){
            Vec3 next=DirectionalGroundNodeEvaluator.entityPosition(path.getNodePos(i),gravity);
            double segment=previous.distanceTo(next);
            if(!Double.isFinite(segment))return Double.POSITIVE_INFINITY;
            result+=segment;previous=next;
        }
        return result;
    }

    private static Vec3 safeFocus(Goal goal){
        try{return goal.focus();}catch(RuntimeException ignored){return null;}
    }

    private static boolean safeSatisfied(Goal goal,Vec3 position,Direction gravity){
        try{return goal.satisfied(position,gravity);}catch(RuntimeException ignored){return false;}
    }

    private static double safeHeuristic(Goal goal,Vec3 position,Direction gravity){
        try{
            double value=goal.heuristic(position,gravity);
            return Double.isFinite(value)&&value>=0.0D?value:Double.POSITIVE_INFINITY;
        }catch(RuntimeException ignored){return Double.POSITIVE_INFINITY;}
    }

    private static Plan noPlan(Mob mob,Path path,Vec3 frontier,double walkingCost,int evaluations){
        Direction gravity=mob==null?Direction.DOWN:GravityDirectionUtil.getOwnGravityDirection(mob);
        Vec3 position=frontier!=null?frontier:mob==null?Vec3.ZERO:mob.position();
        return new Plan(Kind.NO_PLAN,path,position,gravity,null,walkingCost,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,evaluations);
    }

    private static boolean finite(Vec3 value){return value!=null&&Double.isFinite(value.x+value.y+value.z);}
}
