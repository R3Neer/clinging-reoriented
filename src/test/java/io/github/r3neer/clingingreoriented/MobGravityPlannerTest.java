package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class MobGravityPlannerTest {
    @Test void riskIsZeroInsideSafeFallAllowanceAndMonotonicAfterIt(){
        assertEquals(0.0D,MobGravityPlanner.riskCost(3.0D,20.0D),1.0E-12D);
        double mild=MobGravityPlanner.riskCost(4.0D,20.0D);
        double severe=MobGravityPlanner.riskCost(12.0D,20.0D);
        double lethal=MobGravityPlanner.riskCost(24.0D,20.0D);
        assertTrue(mild>0.0D,"first damaging fall must have positive cost");
        assertTrue(mild<severe&&severe<lethal,"risk cost must grow monotonically with impact severity");
        assertTrue(lethal>severe*10.0D,"near/lethal impact needs a strongly non-linear penalty");
    }

    @Test void sameImpactCostsMoreWhenCurrentHealthIsLower(){
        double healthy=MobGravityPlanner.riskCost(10.0D,20.0D);
        double hurt=MobGravityPlanner.riskCost(10.0D,6.0D);
        assertTrue(hurt>healthy,"planner ignored current-health exposure");
    }

    @Test void damageProxyPreservesVanillaThreeBlockAllowance(){
        assertEquals(0.0D,MobGravityPlanner.predictedDamagePoints(2.99D),1.0E-12D);
        assertEquals(0.0D,MobGravityPlanner.predictedDamagePoints(3.0D),1.0E-12D);
        assertEquals(2.5D,MobGravityPlanner.predictedDamagePoints(5.5D),1.0E-12D);
    }

    @Test void malformedRiskInputsFailExpensivelyRatherThanLookingSafe(){
        assertTrue(MobGravityPlanner.riskCost(Double.NaN,20.0D)>100_000.0D);
        assertTrue(MobGravityPlanner.riskCost(10.0D,0.0D)>100_000.0D);
    }
}
