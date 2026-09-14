package io.github.r3neer.clingingreoriented.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Reuses Elytra's own aerodynamic sound language without ever entering FALL_FLYING.
 * The sound is local-only, velocity driven and dies as soon as Gravity Fall gives up
 * presentation ownership.
 */
public final class GravityFallAirSound extends AbstractTickableSoundInstance {
    private static final double START_SPEED_SQR=0.75D*0.75D;
    private static GravityFallAirSound active;

    private final LocalPlayer player;
    private int time;

    private GravityFallAirSound(LocalPlayer player){
        super(SoundEvents.ELYTRA_FLYING,SoundSource.PLAYERS,SoundInstance.createUnseededRandom());
        this.player=player;
        this.looping=true;
        this.delay=0;
        this.volume=0.0F;
        this.pitch=1.0F;
        this.relative=false;
    }

    public static void tick(Minecraft client){
        LocalPlayer player=client==null?null:client.player;
        if(player==null){clear();return;}
        if(active!=null){
            if(active.isStopped())active=null;
            return;
        }
        if(GravityFallVisuals.active(player)&&!player.isFallFlying()&&player.getDeltaMovement().lengthSqr()>=START_SPEED_SQR){
            active=new GravityFallAirSound(player);
            client.getSoundManager().play(active);
        }
    }

    public static void clear(){
        if(active!=null){active.stop();active=null;}
    }

    @Override public void tick(){
        time++;
        if(player.isRemoved()||player.isFallFlying()||!GravityFallVisuals.active(player)){
            stop();return;
        }
        x=(float)player.getX();y=(float)player.getY();z=(float)player.getZ();

        // ElytraOnPlayerSoundInstance uses velocity squared / 4 for volume and raises pitch
        // above the 0.8-volume region. Preserve that familiar mapping so Gravity Fall speaks
        // vanilla's existing "fast air" language instead of inventing a second one.
        float speedSqr=(float)player.getDeltaMovement().lengthSqr();
        volume=speedSqr>=1.0E-7F?Mth.clamp(speedSqr/4.0F,0.0F,1.0F):0.0F;

        // Shorter admission fade than Elytra's takeoff silence: Gravity Fall itself already has
        // a 12-tick airborne gate plus a 6-tick body blend before this feedback can normally start.
        if(time<10)volume*=time/10.0F;
        pitch=volume>0.8F?1.0F+(volume-0.8F):1.0F;
    }
}
