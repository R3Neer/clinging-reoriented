package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class LandingSurfaceApiGameTests {
    private static LandingSurfaceProvider provider(String id) {
        return new LandingSurfaceProvider() {
            @Override public Optional<LocalContact> currentSupport(Query query) {
                return Optional.of(new LocalContact(id, 1L, FaceGeometry.vector(query.gravity().getOpposite())));
            }
            @Override public boolean revalidate(Query query, LocalContact contact) {
                return contact != null && contact.localId().equals(id) && contact.revision() == 1L;
            }
        };
    }

    @GameTest(padding=16)
    public void externalProviderCanDefineRealSupportWithoutVanillaGroundFlag(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(5.5,7.0,5.5)));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(false);
        Identifier id=Identifier.fromNamespaceAndPath("clinging_reoriented_test","air_support");
        var registration=LandingSurfaces.register(id,provider("fixture"));
        try {
            h.assertTrue(AirChanges.grounded(p),"registered surface provider can define support without forging vanilla onGround");
            var contact=LandingSurfaces.currentSupport(p,Direction.DOWN).orElseThrow();
            h.assertTrue(contact.key().provider().equals(id),"contact preserves provider identity");
            h.assertTrue(contact.gravity()==Direction.DOWN,"contact preserves the gravity frame that created it");
            h.assertTrue(LandingSurfaces.revalidate(p,Direction.DOWN,contact),"provider contact revalidates in the same gravity frame");
            h.assertFalse(LandingSurfaces.revalidate(p,Direction.EAST,contact),"stale contact cannot be reused after a gravity-frame change");
        } finally { registration.close(); }
        h.assertFalse(AirChanges.grounded(p),"closing registration removes external support");
        h.succeed();
    }

    @GameTest(padding=16)
    public void brokenProvidersFailClosedAndDoNotReplaceExistingOwners(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(5.5,7.0,5.5)));p.setDeltaMovement(Vec3.ZERO);p.setOnGround(false);
        Identifier id=Identifier.fromNamespaceAndPath("clinging_reoriented_test","broken");
        LandingSurfaceProvider broken=new LandingSurfaceProvider(){
            @Override public Optional<LocalContact> currentSupport(Query query){throw new IllegalStateException("fixture failure");}
            @Override public boolean revalidate(Query query,LocalContact contact){throw new IllegalStateException("fixture failure");}
        };
        var registration=LandingSurfaces.register(id,broken);
        try {
            h.assertFalse(AirChanges.grounded(p),"provider exception fails closed instead of becoming support");
            boolean duplicateRejected=false;
            try { LandingSurfaces.register(id,provider("replacement")); }
            catch(IllegalStateException expected){ duplicateRejected=true; }
            h.assertTrue(duplicateRejected,"duplicate provider id cannot silently replace an owner");
        } finally { registration.close(); }
        h.succeed();
    }

    @GameTest(padding=16)
    public void vanillaProviderPreservesFeetSupportAndRejectsSideContact(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();
        var feet=h.absoluteVec(new Vec3(5.5,6.0,5.5));
        p.snapTo(feet);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);
        var floor=net.minecraft.core.BlockPos.containing(feet.add(0,-.01,0));
        h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        h.assertTrue(LandingSurfaces.currentSupport(p,Direction.DOWN).isPresent(),"vanilla floor remains valid support through provider seam");
        h.getLevel().setBlockAndUpdate(floor,Blocks.AIR.defaultBlockState());
        var body=p.getBoundingBox();
        var side=new net.minecraft.core.BlockPos((int)Math.floor(body.maxX+.01),net.minecraft.core.BlockPos.containing(body.getCenter()).getY(),net.minecraft.core.BlockPos.containing(body.getCenter()).getZ());
        h.getLevel().setBlockAndUpdate(side,Blocks.STONE.defaultBlockState());p.setOnGround(true);
        h.assertTrue(LandingSurfaces.currentSupport(p,Direction.DOWN).isEmpty(),"side/body contact is not feet support");
        h.succeed();
    }
}
