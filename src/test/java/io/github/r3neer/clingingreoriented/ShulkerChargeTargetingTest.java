package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

public class ShulkerChargeTargetingTest {
    @Test void alignedFarTargetBeatsCloserOffAxisTarget(){
        var aligned=ShulkerChargeTargeting.score(new Vec3(1,0,0),new Vec3(20,0,0)).orElseThrow();
        var closeOffAxis=ShulkerChargeTargeting.score(new Vec3(1,0,0),new Vec3(5,0,1)).orElseThrow();
        assertTrue(aligned.compareTo(closeOffAxis)<0,"angular intent outranks mere proximity");
    }
    @Test void outsideConeIsRejected(){assertTrue(ShulkerChargeTargeting.score(new Vec3(1,0,0),new Vec3(5,0,5)).isEmpty());}
    @Test void malformedIntentFailsClosedToForward(){assertEquals(new Vec3(0,0,1),ShulkerChargeTargeting.safeIntent(new Vec3(Double.NaN,0,0)));}
}
