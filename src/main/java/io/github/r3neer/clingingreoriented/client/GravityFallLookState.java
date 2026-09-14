package io.github.r3neer.clingingreoriented.client;

import net.minecraft.client.Minecraft;

/** Re-enters vanilla yaw/pitch coordinates when Gravity Fall releases full-sphere camera ownership. */
public final class GravityFallLookState {
    private static boolean wasActive;

    private GravityFallLookState() {}

    public static void tick(Minecraft client){
        if(client==null||client.player==null){wasActive=false;return;}
        boolean active=GravityFallVisuals.active(client.player);
        if(wasActive&&!active)canonicalize(client);
        wasActive=active;
    }

    public static void clear(){wasActive=false;}

    private static void canonicalize(Minecraft client){
        var player=client.player;
        var mapped=GravityFallLookMath.vanillaEquivalent(player.getYRot(),player.getXRot());
        // Both old/current values are collapsed to the same equivalent direction at the ownership
        // boundary. This avoids a one-frame Euler interpolation across different representations
        // of the same look vector (e.g. +120 pitch vs yaw+180/+60 pitch).
        player.setYRot(mapped.yaw());player.yRotO=mapped.yaw();
        player.setXRot(mapped.pitch());player.xRotO=mapped.pitch();
    }
}
