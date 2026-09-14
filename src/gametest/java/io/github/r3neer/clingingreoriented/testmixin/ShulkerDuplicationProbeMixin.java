package io.github.r3neer.clingingreoriented.testmixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Test-only determinism seam for vanilla's density-based Shulker duplication roll. */
@Mixin(Shulker.class)
public abstract class ShulkerDuplicationProbeMixin {
    private static final String FIXTURE_CLASS_SUFFIX="ShulkerChargeProjectileGameTests$DeterministicTeleportShulker";

    @Redirect(
        method="hitByShulkerBullet",
        at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private float clinging$deterministicDuplicationRoll(RandomSource random){
        Shulker self=(Shulker)(Object)this;
        return self.getClass().getName().endsWith(FIXTURE_CLASS_SUFFIX)?Float.POSITIVE_INFINITY:random.nextFloat();
    }
}
