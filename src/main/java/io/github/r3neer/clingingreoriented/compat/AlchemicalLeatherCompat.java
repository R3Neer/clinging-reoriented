package io.github.r3neer.clingingreoriented.compat;

import io.github.r3neer.clingingreoriented.*;
import java.lang.reflect.Method;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/**
 * Linkage-safe optional bridge: Clinging publishes semantic facts; Alchemical Leather owns
 * source arbitration, armor selection, balance and durability.
 */
public final class AlchemicalLeatherCompat implements ModInitializer {
    private static final Identifier TURN=Identifier.fromNamespaceAndPath(ClingingReoriented.ID,"gravity_turn");
    private static final Identifier FLIGHT=Identifier.fromNamespaceAndPath(ClingingReoriented.ID,"controlled_flight_tick");
    private static final Identifier CLINGING=Identifier.fromNamespaceAndPath("alexsmobs","clinging");
    private static Method emit;

    @Override public void onInitialize(){
        if(!FabricLoader.getInstance().isModLoaded("alchemical_leather"))return;
        try{
            var api=Class.forName("io.github.r3neer.alchemicalleather.api.InfusionWearApi",false,AlchemicalLeatherCompat.class.getClassLoader());
            emit=api.getMethod("emit",LivingEntity.class,Holder.class,Identifier.class,double.class);
            ServerTickEvents.END_SERVER_TICK.register(server->{
                for(var player:server.getPlayerList().getPlayers())controlledFlightTick(player);
            });
        }catch(ReflectiveOperationException|LinkageError failure){
            emit=null;
            org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID).warn("Alchemical Leather is present but its infusion-wear API is unavailable; compatibility disabled",failure);
        }
    }

    /** Called only after the authoritative gravity-attempt path returned SUCCESS. */
    public static void successfulTurn(ServerPlayer player){
        if(emit==null||player==null)return;
        Holder<MobEffect> effect=turnEffect(player);
        if(effect!=null)publish(player,effect,TURN,1.0);
    }

    static Holder<MobEffect> turnEffect(ServerPlayer player){
        if(player==null)return null;
        if(player.hasEffect(Reorientation.EFFECT))return Reorientation.EFFECT;
        Holder<MobEffect> clinging=clinging();
        return clinging!=null&&player.hasEffect(clinging)?clinging:null;
    }

    private static void controlledFlightTick(ServerPlayer player){
        if(emit==null||!controlledFlightEligible(player))return;
        publish(player,Reorientation.EFFECT,FLIGHT,1.0);
    }

    static boolean controlledFlightEligible(ServerPlayer player){
        if(player==null||!player.isAlive()||player.isSpectator()||player.isSleeping())return false;
        if(!player.hasEffect(Reorientation.EFFECT)||player.isPassenger()||player.isFallFlying()||player.getAbilities().flying)return false;
        if(FluidContext.intersects(player))return false;
        if(!ClingingReoriented.controlsPhysics(player)||AirChanges.grounded(player))return false;
        var state=ClingingReoriented.data(player);
        if(state.groundedOnSurface)return false;
        return !AnatomyBridge.active(player)||!AnatomyBridge.supported(player);
    }

    private static Holder<MobEffect> clinging(){return BuiltInRegistries.MOB_EFFECT.get(CLINGING).map(value->(Holder<MobEffect>)value).orElse(null);}

    private static void publish(ServerPlayer player,Holder<MobEffect> effect,Identifier event,double amount){
        try{emit.invoke(null,player,effect,event,amount);}
        catch(ReflectiveOperationException|RuntimeException failure){
            org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID).warn("Disabling failed Alchemical Leather infusion-wear bridge",failure);
            emit=null;
        }
    }
}
