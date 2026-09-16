package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class MobGravityPlanningBudgetTest {
    @Test void regionalBurstIsCappedAndResetsNextTick(){
        var budget=new MobGravityPlanningBudget.LevelBudget();long tick=100L;
        for(int i=0;i<MobGravityPlanningBudget.REGION_PLANS_PER_TICK;i++)
            assertTrue(budget.tryAcquire(tick,10,10),"regional token "+i+" rejected too early");
        assertFalse(budget.tryAcquire(tick,11,12),"regional cap did not reject same-region burst");
        assertTrue(budget.tryAcquire(tick,128,10),"regional rejection incorrectly consumed another region's capacity");
        assertTrue(budget.tryAcquire(tick+1,10,10),"budget did not reset on next game tick");
    }

    @Test void globalBurstIsBoundedAcrossManyRegions(){
        var budget=new MobGravityPlanningBudget.LevelBudget();long tick=200L;
        for(int i=0;i<MobGravityPlanningBudget.GLOBAL_PLANS_PER_TICK;i++){
            int x=i<<MobGravityPlanningBudget.REGION_SHIFT;
            assertTrue(budget.tryAcquire(tick,x,0),"global token "+i+" rejected too early");
        }
        int next=MobGravityPlanningBudget.GLOBAL_PLANS_PER_TICK<<MobGravityPlanningBudget.REGION_SHIFT;
        assertFalse(budget.tryAcquire(tick,next,0),"global cap allowed an unbounded same-tick planning burst");
        assertTrue(budget.tryAcquire(tick+1,next,0),"global budget did not replenish next tick");
    }

    @Test void regionKeysRemainStableAcrossNegativeCoordinates(){
        long a=MobGravityPlanningBudget.regionKey(-1,-1);
        assertEquals(a,MobGravityPlanningBudget.regionKey(-64,-64));
        assertNotEquals(a,MobGravityPlanningBudget.regionKey(-65,-64));
        assertNotEquals(a,MobGravityPlanningBudget.regionKey(-64,-65));
    }
}
