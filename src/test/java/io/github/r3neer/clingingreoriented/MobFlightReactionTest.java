package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class MobFlightReactionTest {
    @Test void smallTargetJitterNeverStartsStrategicReaction(){
        var state=new MobFlightReaction.State();MobFlightReaction.begin(state,null,new Vec3(0,0,0));
        MobFlightReaction.observeTarget(state,100L,6,new Vec3(1.0,0,0));
        assertFalse(state.targetPending());
        assertFalse(MobFlightReaction.targetReady(state,200L));
    }

    @Test void materialTargetMovementKeepsFirstDeadlineWhileObservationUpdates(){
        var state=new MobFlightReaction.State();MobFlightReaction.begin(state,null,new Vec3(0,0,0));
        MobFlightReaction.observeTarget(state,100L,6,new Vec3(3,0,0));
        assertEquals(106L,state.targetReactAt());
        MobFlightReaction.observeTarget(state,103L,6,new Vec3(5,0,0));
        assertEquals(106L,state.targetReactAt(),"continuing target movement postponed reaction forever");
        assertEquals(new Vec3(5,0,0),state.pendingTarget());
        assertFalse(MobFlightReaction.targetReady(state,105L));
        assertTrue(MobFlightReaction.targetReady(state,106L));
        assertEquals(new Vec3(5,0,0),MobFlightReaction.consumeTarget(state));
        assertEquals(new Vec3(5,0,0),state.targetAnchor());
        assertFalse(state.targetPending());
    }

    @Test void returningInsideAnchorBeforeDeadlineCancelsTargetReaction(){
        var state=new MobFlightReaction.State();MobFlightReaction.begin(state,null,new Vec3(0,0,0));
        MobFlightReaction.observeTarget(state,20L,5,new Vec3(4,0,0));
        assertTrue(state.targetPending());
        MobFlightReaction.observeTarget(state,23L,5,new Vec3(.5,0,0));
        assertFalse(state.targetPending());
        assertFalse(MobFlightReaction.targetReady(state,30L));
    }

    @Test void dangerUsesFirstDeadlineAndDisappearingDangerCancelsGhostAction(){
        var state=new MobFlightReaction.State();MobFlightReaction.begin(state,null,null);
        var blocking=new MobFlightMonitor.Observation(MobFlightMonitor.Status.BLOCKING_CONTACT,null);
        var trapped=new MobFlightMonitor.Observation(MobFlightMonitor.Status.TRAPPED_CONTACT,null);
        var clear=new MobFlightMonitor.Observation(MobFlightMonitor.Status.CLEAR,null);
        MobFlightReaction.observeDanger(state,50L,7,blocking);
        assertEquals(57L,state.dangerReactAt());
        MobFlightReaction.observeDanger(state,53L,7,trapped);
        assertEquals(57L,state.dangerReactAt(),"changing danger type postponed the original reaction deadline");
        assertEquals(MobFlightMonitor.Status.TRAPPED_CONTACT,state.pendingDanger());
        MobFlightReaction.observeDanger(state,55L,7,clear);
        assertFalse(state.dangerPending());
        assertFalse(MobFlightReaction.dangerReady(state,80L));
    }

    @Test void persistentDangerBecomesReadyOnlyAtConfiguredDelay(){
        var state=new MobFlightReaction.State();MobFlightReaction.begin(state,null,null);
        var danger=new MobFlightMonitor.Observation(MobFlightMonitor.Status.UNKNOWN_GEOMETRY,null);
        MobFlightReaction.observeDanger(state,10L,4,danger);
        assertFalse(MobFlightReaction.dangerReady(state,13L));
        assertTrue(MobFlightReaction.dangerReady(state,14L));
        assertEquals(MobFlightMonitor.Status.UNKNOWN_GEOMETRY,MobFlightReaction.consumeDanger(state));
        assertFalse(state.dangerPending());
    }
}
