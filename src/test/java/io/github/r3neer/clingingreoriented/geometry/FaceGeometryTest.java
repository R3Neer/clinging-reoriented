package io.github.r3neer.clingingreoriented.geometry;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FaceGeometryTest {
    @Test void allFacesPointOutwardAndProduceOppositeGravity(){
        AABB surface=new AABB(0,0,0,1,1,1);
        for(Direction normal:Direction.values()){
            Vec3 center=surface.getCenter().add(FaceGeometry.vector(normal).scale(.75));
            var face=FaceGeometry.face(AABB.ofSize(center,.5,.5,.5),surface,normal);
            assertNotNull(face);assertEquals(0,face.distance(),1e-8);
            assertTrue(FaceGeometry.touching(AABB.ofSize(center,.5,.5,.5),surface,normal.getOpposite()));
        }
    }
    @Test void cannotSelectFaceFromInside(){
        for(Direction n:Direction.values())assertNull(FaceGeometry.face(new AABB(.2,.2,.2,.8,.8,.8),new AABB(0,0,0,1,1,1),n));
    }
    @Test void diagonalNearCornerIsNotPhysicalSupport(){
        assertFalse(FaceGeometry.touching(new AABB(1,1,0,2,2,1),new AABB(0,0,0,1,1,1),Direction.DOWN));
    }
    @Test void tinyReachAndGiantReachAreBounded(){
        assertEquals(.625,FaceGeometry.reach(.6),1e-8);
        assertTrue(FaceGeometry.reach(.6*.28)<.25);
        assertEquals(.9,FaceGeometry.reach(20),1e-8);
    }
    @Test void thirtyRotationsKeepVolumeAndUseActualDimensions(){
        for(float scale:new float[]{.28f,.52f,1f,3.88f})for(Direction old:Direction.values())for(Direction next:Direction.values()){
            if(old==next)continue;
            var dim=EntityDimensions.scalable(.6f*scale,1.8f*scale);
            var a=RotationUtil.makeBoxFromDimensions(dim,old,Vec3.ZERO);
            var b=RotationUtil.makeBoxFromDimensions(dim,next,Vec3.ZERO);
            assertEquals(a.getXsize()*a.getYsize()*a.getZsize(),b.getXsize()*b.getYsize()*b.getZsize(),1e-5);
        }
    }
}
