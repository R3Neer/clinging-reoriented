package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RecoveryBudgetTest {
    @Test void budgetedOffsetsExactlyMatchLegacyRadiusYxzOrder(){
        List<RecoveryBudget.Offset> legacy=new ArrayList<>();
        double maxDistanceSqr=4.0D*4.0D+1.0E-9D;
        for(int radius=1;radius<=8;radius++)for(int y=-radius;y<=radius;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=radius)continue;
            double dx=x*.5D,dy=y*.5D,dz=z*.5D;
            if(dx*dx+dy*dy+dz*dz<=maxDistanceSqr)legacy.add(new RecoveryBudget.Offset(x,y,z));
        }
        assertEquals(2108,legacy.size(),"legacy recovery candidate count changed unexpectedly");
        assertEquals(legacy,RecoveryBudget.offsetsForTest(),"budgeted recovery changed candidate set or ordering");
        assertEquals(64,RecoveryBudget.CANDIDATES_PER_CALL,"recovery per-call budget drifted");
    }
}
