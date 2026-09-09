package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Connection.class)
public abstract class OutgoingMoveMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;)V",at=@At("HEAD"))
    private void clinging$reference(Packet<?> packet,CallbackInfo ci){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null || AnatomyBridge.active(p) || !mc.isSameThread() || !(packet instanceof ServerboundMovePlayerPacket move) || !move.hasPosition() || !ClingingReoriented.managed(p))return;
        var s=ClingingReoriented.data(p);var surface=MovingSurface.resolve(p);
        if(!s.groundedOnSurface || surface==null || !ClientPlayNetworking.canSend(Payloads.MoveReference.TYPE))return;
        ClientPlayNetworking.send(new Payloads.MoveReference(s.supportId,s.revision,new Vec3(move.getX(p.getX()),move.getY(p.getY()),move.getZ(p.getZ())),surface.position()));
    }
}
