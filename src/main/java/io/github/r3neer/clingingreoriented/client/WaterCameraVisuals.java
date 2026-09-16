package io.github.r3neer.clingingreoriented.client;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.WaterSupportProbe;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Local-camera-only water presentation. It never changes logical gravity or Gravity Changer's animation state. */
public final class WaterCameraVisuals {
    private static final Vec3 WORLD_UP=new Vec3(0,1,0);
    private static final int SUPPORT_CONFIRM_TICKS=2;
    private static final int SUPPORT_RELEASE_TICKS=2;
    private static final double MAX_ROLL_RADIANS_PER_SECOND=Math.toRadians(360.0D);
    private static final double RELEASE_EPS=Math.toRadians(.15D);

    private static Player owner;
    private static boolean active;
    private static boolean releasing;
    private static Direction observedGravity;
    private static Vec3 supportUp;
    private static int supportHits;
    private static int supportMisses;
    private static boolean supported;
    private static int lastLogicTick=Integer.MIN_VALUE;
    private static long lastRenderNanos;
    private static double roll;

    private WaterCameraVisuals() {}

    /** Called after vanilla + Gravity Changer have produced the base camera quaternion. */
    public static boolean apply(Player player,Quaternionf base,long nowNanos){
        if(player==null||base==null){clear();return false;}
        updateMode(player);
        if(owner!=player||(!active&&!releasing))return false;

        double target;
        if(releasing)target=0.0D;
        else{
            Vec3 desired=supported&&supportUp!=null?supportUp:WORLD_UP;
            target=WaterCameraMath.targetRoll(base,desired);
            if(!Double.isFinite(target))target=roll; // roll is undefined when looking exactly along desired-up.
        }

        double dt=0.0D;
        if(lastRenderNanos>0L&&nowNanos>lastRenderNanos)dt=Math.min(.10D,(nowNanos-lastRenderNanos)/1_000_000_000.0D);
        lastRenderNanos=nowNanos;
        roll=WaterCameraMath.stepAngle(roll,target,MAX_ROLL_RADIANS_PER_SECOND*dt);
        base.set(WaterCameraMath.applyRoll(base,roll));

        if(releasing&&Math.abs(roll)<=RELEASE_EPS){
            roll=0.0D;owner=null;active=false;releasing=false;observedGravity=null;supportUp=null;
            supportHits=0;supportMisses=0;supported=false;lastLogicTick=Integer.MIN_VALUE;lastRenderNanos=0L;
        }
        return true;
    }

    public static boolean active(Player player){return owner==player&&active;}
    public static boolean supported(Player player){return owner==player&&active&&supported;}
    public static Vec3 targetUp(Player player){return owner==player&&active?(supported&&supportUp!=null?supportUp:WORLD_UP):null;}
    public static void clear(){owner=null;active=false;releasing=false;observedGravity=null;supportUp=null;supportHits=0;supportMisses=0;supported=false;lastLogicTick=Integer.MIN_VALUE;lastRenderNanos=0L;roll=0.0D;}

    private static void updateMode(Player player){
        boolean shouldOwn=player.isInWater()&&ClingingReoriented.controlsPhysics(player);
        if(owner!=player){
            if(!shouldOwn){clear();return;}
            owner=player;active=true;releasing=false;observedGravity=null;supportUp=null;supportHits=0;supportMisses=0;supported=false;lastLogicTick=Integer.MIN_VALUE;lastRenderNanos=0L;roll=0.0D;
        }else if(shouldOwn){active=true;releasing=false;}
        else if(active){active=false;releasing=true;supportUp=null;supportHits=0;supportMisses=0;supported=false;}

        if(!active)return;
        int tick=player.tickCount;
        if(tick==lastLogicTick)return;
        lastLogicTick=tick;
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        if(observedGravity!=gravity){observedGravity=gravity;supportUp=null;supportHits=0;supportMisses=0;supported=false;}

        var contact=WaterSupportProbe.current(player);
        if(contact.isPresent()){
            supportMisses=0;supportHits=Math.min(1_000_000,supportHits+1);supportUp=contact.get().normal();
            if(supportHits>=SUPPORT_CONFIRM_TICKS)supported=true;
        }else{
            supportHits=0;
            if(supported){supportMisses=Math.min(1_000_000,supportMisses+1);if(supportMisses>=SUPPORT_RELEASE_TICKS){supported=false;supportUp=null;supportMisses=0;}}
            else{supportMisses=0;supportUp=null;}
        }
    }
}
