package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class MobReactionTimeTest {
    @Test void reactionIsBoundedAndNeverInstant(){
        assertEquals(MobReactionTime.MAX_TICKS,MobReactionTime.ticksForBaseSpeed(0.0D));
        assertEquals(MobReactionTime.MAX_TICKS,MobReactionTime.ticksForBaseSpeed(Double.NaN));
        assertTrue(MobReactionTime.ticksForBaseSpeed(100.0D)>=MobReactionTime.MIN_TICKS);
        assertTrue(MobReactionTime.ticksForBaseSpeed(100.0D)>0);
    }

    @Test void fasterBaseMovementMeansNoSlowerReaction(){
        double[] speeds={.05D,.10D,.20D,.23D,.30D,.40D,.50D,.80D,2.0D};
        int previous=MobReactionTime.MAX_TICKS;
        for(double speed:speeds){
            int ticks=MobReactionTime.ticksForBaseSpeed(speed);
            assertTrue(ticks<=previous,"reaction got slower when base movement speed increased at "+speed);
            previous=ticks;
        }
    }

    @Test void ordinaryMobSpeedsStayHumanReadableRatherThanExtreme(){
        int slow=MobReactionTime.ticksForBaseSpeed(.10D);
        int ordinary=MobReactionTime.ticksForBaseSpeed(.25D);
        int agile=MobReactionTime.ticksForBaseSpeed(.50D);
        assertTrue(slow>ordinary&&ordinary>agile,"curve does not distinguish slow/ordinary/agile base speeds");
        assertTrue(ordinary>=4&&ordinary<=8,"ordinary mob reaction is implausibly extreme: "+ordinary);
    }

    @Test void curveSaturatesInsteadOfRewardingAbsurdStatsLinearly(){
        int fast=MobReactionTime.ticksForBaseSpeed(1.0D);
        int absurd=MobReactionTime.ticksForBaseSpeed(100.0D);
        assertTrue(absurd>=MobReactionTime.MIN_TICKS,"absurd speed broke minimum latency");
        assertTrue(fast-absurd<=2,"high speed kept buying large linear reaction gains");
    }

    @Test void flightMonitorOnlyLooksFarEnoughToUseReactionWindow(){
        for(int reaction=MobReactionTime.MIN_TICKS;reaction<=MobReactionTime.MAX_TICKS;reaction++){
            int horizon=MobFlightReactor.monitorHorizonForReactionTicks(reaction);
            assertTrue(horizon>reaction,"monitor left no maneuver margin at reaction="+reaction);
            assertTrue(horizon<=MobFlightMonitor.DEFAULT_HORIZON_TICKS,"monitor exceeded its hard horizon");
            assertEquals(Math.min(MobFlightMonitor.DEFAULT_HORIZON_TICKS,reaction+MobFlightReactor.REACTION_MARGIN_TICKS),horizon);
        }
        int ordinaryReaction=MobReactionTime.ticksForBaseSpeed(.25D);
        assertTrue(MobFlightReactor.monitorHorizonForReactionTicks(ordinaryReaction)<MobFlightMonitor.DEFAULT_HORIZON_TICKS,
            "ordinary mobs still pay the old fixed 20-tick monitor cost");
    }
}
