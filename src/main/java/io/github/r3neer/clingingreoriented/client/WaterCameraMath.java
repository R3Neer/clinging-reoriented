package io.github.r3neer.clingingreoriented.client;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure roll correction: preserve camera forward exactly while choosing which world vector is screen-up. */
public final class WaterCameraMath {
    private static final double EPS_SQR=1.0E-12D;
    private WaterCameraMath() {}

    public static Vec3 forward(Quaternionf rotation){return axis(rotation,0.0F,0.0F,-1.0F);}
    public static Vec3 up(Quaternionf rotation){return axis(rotation,0.0F,1.0F,0.0F);}

    /** Signed world-axis roll needed to align screen-up with desiredUp projected around forward. */
    public static double targetRoll(Quaternionf base,Vec3 desiredUp){
        if(!finite(base)||!finite(desiredUp)||desiredUp.lengthSqr()<=EPS_SQR)return Double.NaN;
        Vec3 forward=forward(base);Vec3 currentUp=up(base);
        if(forward.lengthSqr()<=EPS_SQR||currentUp.lengthSqr()<=EPS_SQR)return Double.NaN;
        forward=forward.normalize();currentUp=currentUp.normalize();
        Vec3 wanted=desiredUp.normalize().subtract(forward.scale(desiredUp.normalize().dot(forward)));
        if(wanted.lengthSqr()<=EPS_SQR)return Double.NaN;
        wanted=wanted.normalize();
        double sin=forward.dot(currentUp.cross(wanted));
        double cos=Math.max(-1.0D,Math.min(1.0D,currentUp.dot(wanted)));
        return Math.atan2(sin,cos);
    }

    public static Quaternionf applyRoll(Quaternionf base,double angle){
        if(!finite(base)||!Double.isFinite(angle))return base==null?new Quaternionf():new Quaternionf(base);
        Vec3 forward=forward(base);
        if(forward.lengthSqr()<=EPS_SQR)return new Quaternionf(base);
        forward=forward.normalize();
        return new Quaternionf().rotateAxis((float)angle,(float)forward.x,(float)forward.y,(float)forward.z)
            .mul(new Quaternionf(base)).normalize();
    }

    /** Advance along the shortest angular arc without overshoot. */
    public static double stepAngle(double current,double target,double maxStep){
        if(!Double.isFinite(current)||!Double.isFinite(target)||!Double.isFinite(maxStep)||maxStep<0.0D)return current;
        double delta=Math.atan2(Math.sin(target-current),Math.cos(target-current));
        if(Math.abs(delta)<=maxStep)return current+delta;
        return current+Math.copySign(maxStep,delta);
    }

    private static Vec3 axis(Quaternionf rotation,float x,float y,float z){
        if(!finite(rotation))return Vec3.ZERO;
        Vector3f value=new Quaternionf(rotation).normalize().transform(new Vector3f(x,y,z));
        return new Vec3(value.x,value.y,value.z);
    }
    private static boolean finite(Quaternionf q){return q!=null&&Float.isFinite(q.x()+q.y()+q.z()+q.w());}
    private static boolean finite(Vec3 v){return v!=null&&Double.isFinite(v.x+v.y+v.z);}
}
