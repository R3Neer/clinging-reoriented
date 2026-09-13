package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class BodyOrientationTest {
    private static final double EPS=1.0E-5D;

    @Test void quarterTurnTransportsUpWithoutInventingTwist(){
        var state=BodyOrientation.start(new Quaternionf(),new Vec3(0,1,0));
        var turned=BodyOrientation.transport(state,new Vec3(1,0,0));
        assertVec(new Vec3(1,0,0),BodyOrientation.bodyUp(turned.orientation()));
        assertVec(new Vec3(0,0,-1),BodyOrientation.bodyForward(turned.orientation()));
    }

    @Test void zeroSpeedHoldsFrameAndDirectionExactly(){
        var start=BodyOrientation.start(new Quaternionf().rotateY(.37f),new Vec3(1,0,0));
        var held=BodyOrientation.transport(start,new Vec3(1.0E-5,0,0));
        assertQuaternion(start.orientation(),held.orientation());
        assertVec(start.direction(),held.direction());
    }

    @Test void reservedEastZeroWestHoldoutDoesNotMoveUntilDirectionReturns(){
        var east=BodyOrientation.start(new Quaternionf().rotateY(.61f),new Vec3(1,0,0));
        var zero=BodyOrientation.transport(east,Vec3.ZERO);
        assertQuaternion(east.orientation(),zero.orientation());
        assertVec(new Vec3(1,0,0),zero.direction());

        var west=BodyOrientation.transport(zero,new Vec3(-1,0,0));
        assertVec(new Vec3(-1,0,0),BodyOrientation.bodyUp(west.orientation()));
        assertVec(new Vec3(-1,0,0),west.direction());
        assertTrue(Math.abs(BodyOrientation.bodyForward(west.orientation()).length()-1.0D)<EPS,"reverse lost a normalized twist axis");
    }

    @Test void firstReliableVelocityAfterStationaryStartAlignsFromDisplayedFrame(){
        var displayed=new Quaternionf().rotateY(.8f).rotateX(.2f);
        var waiting=BodyOrientation.start(displayed,Vec3.ZERO);
        assertNull(waiting.direction());
        assertQuaternion(displayed,waiting.orientation());

        var falling=BodyOrientation.transport(waiting,new Vec3(0,-2,0));
        assertVec(new Vec3(0,-1,0),BodyOrientation.bodyUp(falling.orientation()));
    }

    @Test void nearOppositeSamplesStayFiniteAndContinuous(){
        var state=BodyOrientation.start(new Quaternionf(),new Vec3(1,0,0));
        var almostWest=BodyOrientation.transport(state,new Vec3(-1,1.0E-4,0));
        Quaternionf q=almostWest.orientation();
        assertTrue(Float.isFinite(q.x)&&Float.isFinite(q.y)&&Float.isFinite(q.z)&&Float.isFinite(q.w));
        assertVec(new Vec3(-1,1.0E-4,0).normalize(),BodyOrientation.bodyUp(q));
    }

    private static void assertVec(Vec3 expected,Vec3 actual){
        assertNotNull(actual);
        assertTrue(expected.distanceTo(actual)<EPS,"expected "+expected+" but got "+actual);
    }

    private static void assertQuaternion(Quaternionf expected,Quaternionf actual){
        Quaternionf a=new Quaternionf(expected).normalize(),b=new Quaternionf(actual).normalize();
        assertTrue(Math.abs(Math.abs(a.dot(b))-1.0F)<1.0E-5F,"expected equivalent quaternion "+a+" but got "+b);
    }
}