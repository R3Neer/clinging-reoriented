package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GravityBreadcrumbsTest {
    private static final double EPS=1.0E-12;

    @Test void projectionRetainsOnlyThePetsGravityAxisCoordinate(){
        Vec3 pet=new Vec3(2,3,5),breadcrumb=new Vec3(11,13,17);
        for(Direction gravity:new Direction[]{Direction.EAST,Direction.WEST})
            assertVec(new Vec3(2,13,17),GravityBreadcrumbs.projectedTarget(pet,breadcrumb,gravity));
        for(Direction gravity:new Direction[]{Direction.UP,Direction.DOWN})
            assertVec(new Vec3(11,3,17),GravityBreadcrumbs.projectedTarget(pet,breadcrumb,gravity));
        for(Direction gravity:new Direction[]{Direction.NORTH,Direction.SOUTH})
            assertVec(new Vec3(11,13,5),GravityBreadcrumbs.projectedTarget(pet,breadcrumb,gravity));
    }

    @Test void movementPlaneDistanceIgnoresSeparationAlongGravity(){
        Vec3 pet=new Vec3(2,3,5);
        assertEquals(0,GravityBreadcrumbs.movementPlaneDistanceSqr(pet,new Vec3(200,3,5),Direction.EAST),EPS);
        assertEquals(0,GravityBreadcrumbs.movementPlaneDistanceSqr(pet,new Vec3(2,-300,5),Direction.DOWN),EPS);
        assertEquals(0,GravityBreadcrumbs.movementPlaneDistanceSqr(pet,new Vec3(2,3,900),Direction.NORTH),EPS);
        assertEquals(225,GravityBreadcrumbs.movementPlaneDistanceSqr(pet,new Vec3(11,13,17),Direction.DOWN),EPS);
    }

    private static void assertVec(Vec3 expected,Vec3 actual){
        assertEquals(expected.x,actual.x,EPS);assertEquals(expected.y,actual.y,EPS);assertEquals(expected.z,actual.z,EPS);
    }
}
