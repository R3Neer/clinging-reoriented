package io.github.r3neer.clingingreoriented.geometry;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Pure continuous collision test for an axis-aligned body translated between two positions. */
public final class SweptAabb {
    private static final double EPS=1.0E-9D;
    public record Hit(double fraction,Direction normal) {}
    private record Axis(double enter,double exit,Direction normal) {}
    private SweptAabb() {}

    public static Optional<Hit> hit(AABB start,AABB end,AABB obstacle){
        if(!finite(start)||!finite(end)||!finite(obstacle)||!sameSize(start,end))return Optional.empty();
        Vec3 delta=new Vec3(end.minX-start.minX,end.minY-start.minY,end.minZ-start.minZ);
        Axis x=axis(start.minX,start.maxX,obstacle.minX,obstacle.maxX,delta.x,Direction.WEST,Direction.EAST);
        Axis y=axis(start.minY,start.maxY,obstacle.minY,obstacle.maxY,delta.y,Direction.DOWN,Direction.UP);
        Axis z=axis(start.minZ,start.maxZ,obstacle.minZ,obstacle.maxZ,delta.z,Direction.NORTH,Direction.SOUTH);
        if(x==null||y==null||z==null)return Optional.empty();
        double enter=Math.max(x.enter(),Math.max(y.enter(),z.enter()));
        double exit=Math.min(x.exit(),Math.min(y.exit(),z.exit()));
        if(enter>exit+EPS||exit<-EPS||enter>1.0D+EPS||enter<-EPS)return Optional.empty();
        Direction normal=normalAt(enter,x,y,z,null);
        return normal==null?Optional.empty():Optional.of(new Hit(Math.max(0.0D,Math.min(1.0D,enter)),normal));
    }

    /** Like {@link #hit}, but accepts a corner/edge tie only when the requested normal participates in the first contact. */
    public static Optional<Hit> hitForNormal(AABB start,AABB end,AABB obstacle,Direction desired){
        if(desired==null||!finite(start)||!finite(end)||!finite(obstacle)||!sameSize(start,end))return Optional.empty();
        Vec3 delta=new Vec3(end.minX-start.minX,end.minY-start.minY,end.minZ-start.minZ);
        Axis x=axis(start.minX,start.maxX,obstacle.minX,obstacle.maxX,delta.x,Direction.WEST,Direction.EAST);
        Axis y=axis(start.minY,start.maxY,obstacle.minY,obstacle.maxY,delta.y,Direction.DOWN,Direction.UP);
        Axis z=axis(start.minZ,start.maxZ,obstacle.minZ,obstacle.maxZ,delta.z,Direction.NORTH,Direction.SOUTH);
        if(x==null||y==null||z==null)return Optional.empty();
        double enter=Math.max(x.enter(),Math.max(y.enter(),z.enter()));
        double exit=Math.min(x.exit(),Math.min(y.exit(),z.exit()));
        if(enter>exit+EPS||exit<-EPS||enter>1.0D+EPS||enter<-EPS)return Optional.empty();
        Direction normal=normalAt(enter,x,y,z,desired);
        return normal==desired?Optional.of(new Hit(Math.max(0.0D,Math.min(1.0D,enter)),desired)):Optional.empty();
    }

    private static Direction normalAt(double enter,Axis x,Axis y,Axis z,Direction desired){
        if(desired!=null){
            for(Axis axis:new Axis[]{x,y,z})if(axis.normal()==desired&&Math.abs(axis.enter()-enter)<=EPS)return desired;
            return null;
        }
        for(Axis axis:new Axis[]{x,y,z})if(axis.normal()!=null&&Math.abs(axis.enter()-enter)<=EPS)return axis.normal();
        return null;
    }

    private static Axis axis(double min,double max,double omin,double omax,double delta,Direction negativeNormal,Direction positiveNormal){
        if(Math.abs(delta)<=EPS){
            if(max<=omin+EPS||min>=omax-EPS)return null;
            return new Axis(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,null);
        }
        if(delta>0.0D)return new Axis((omin-max)/delta,(omax-min)/delta,negativeNormal);
        return new Axis((omax-min)/delta,(omin-max)/delta,positiveNormal);
    }

    private static boolean sameSize(AABB a,AABB b){return Math.abs(a.getXsize()-b.getXsize())<=EPS&&Math.abs(a.getYsize()-b.getYsize())<=EPS&&Math.abs(a.getZsize()-b.getZsize())<=EPS;}
    private static boolean finite(AABB b){return b!=null&&Double.isFinite(b.minX+b.minY+b.minZ+b.maxX+b.maxY+b.maxZ);}
}
