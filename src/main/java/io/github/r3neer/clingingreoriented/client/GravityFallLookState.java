package io.github.r3neer.clingingreoriented.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

/** Owns local Gravity Fall camera orientation while keeping entity yaw/pitch as the forward-vector gauge. */
public final class GravityFallLookState {
    private static Player owner;
    private static Quaternionf baseRotation;
    private static float gaugeYaw;
    private static boolean wasActive;

    private GravityFallLookState() {}

    public static void tick(Minecraft client){
        if(client==null||client.player==null){clear();return;}
        Player player=client.player;
        boolean active=GravityFallVisuals.active(player);
        if(active){ensure(player);synchronizeExternalYawGauge(player);wasActive=true;return;}
        if(wasActive&&owner==player&&baseRotation!=null)writeForwardGauge(player,baseRotation);
        owner=null;baseRotation=null;gaugeYaw=0.0F;wasActive=false;
    }

    /** Called from the Entity.turn interception. Mouse deltas retain vanilla scale but use screen axes. */
    public static void turn(Player player,double horizontalInput,double verticalInput){
        if(player==null||!GravityFallVisuals.active(player))return;
        float horizontal=(float)horizontalInput*0.15F;
        float vertical=(float)verticalInput*0.15F;
        if(!Float.isFinite(horizontal)||!Float.isFinite(vertical))return;
        ensure(player);
        synchronizeExternalYawGauge(player);
        baseRotation=GravityFallLookMath.screenTurn(baseRotation,horizontal,vertical);
        writeForwardGauge(player,baseRotation);
    }

    /** Camera mixin consumes a defensive copy; null means vanilla/Gravity Changer owns the camera normally. */
    public static Quaternionf cameraBase(Player player){
        Minecraft mc=Minecraft.getInstance();
        if(player==null||mc.player!=player||!GravityFallVisuals.active(player))return null;
        ensure(player);
        synchronizeExternalYawGauge(player);
        return new Quaternionf(baseRotation);
    }

    /** Test/repair hook: rebuild camera orientation from the entity's current vanilla-compatible forward gauge. */
    public static void reseed(Player player){
        Minecraft mc=Minecraft.getInstance();
        if(player==null||mc.player!=player||!GravityFallVisuals.active(player))return;
        owner=player;
        gaugeYaw=player.getYRot();
        baseRotation=GravityFallLookMath.vanillaRotation(gaugeYaw,player.getXRot());
        wasActive=true;
    }

    public static void clear(){owner=null;baseRotation=null;gaugeYaw=0.0F;wasActive=false;}

    private static void ensure(Player player){
        if(owner==player&&baseRotation!=null)return;
        owner=player;
        gaugeYaw=player.getYRot();
        baseRotation=GravityFallLookMath.vanillaRotation(gaugeYaw,player.getXRot());
    }

    /**
     * Gravity transitions deliberately change the entity yaw gauge while compensating visual gravity.
     * Detect that external gauge edit from the local player itself and re-express our base quaternion,
     * preserving the exact world camera without coupling this state machine to transition packet code.
     */
    private static void synchronizeExternalYawGauge(Player player){
        float actual=player.getYRot();
        if(!Float.isFinite(actual)){return;}
        float delta=Mth.wrapDegrees(actual-gaugeYaw);
        if(Math.abs(delta)>1.0E-4F)baseRotation=GravityFallLookMath.rebaseYaw(baseRotation,delta);
        gaugeYaw=actual;
    }

    private static void writeForwardGauge(Player player,Quaternionf rotation){
        var mapped=GravityFallLookMath.forwardEquivalent(rotation,gaugeYaw);
        player.setYRot(mapped.yaw());player.yRotO=mapped.yaw();
        player.setXRot(mapped.pitch());player.xRotO=mapped.pitch();
        gaugeYaw=mapped.yaw();
    }
}
