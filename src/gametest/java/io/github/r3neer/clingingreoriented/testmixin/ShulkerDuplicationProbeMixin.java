package io.github.r3neer.clingingreoriented.testmixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Test-only determinism seam for vanilla's density-based Shulker duplication roll. */
@Mixin(Shulker.class)
public abstract class ShulkerDuplicationProbeMixin {
    private static final String CLINGING_FORCE_DUPLICATION_TAG="clinging_reoriented_test_force_duplication";

    @Redirect(
        method="hitByShulkerBullet",
        at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private float clinging$deterministicDuplicationRoll(RandomSource random){
        Shulker self=(Shulker)(Object)this;
        return self.getTags().contains(CLINGING_FORCE_DUPLICATION_TAG)?1.0F:random.nextFloat();
    }
}
