package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ConnectionMixin {
    @org.spongepowered.asm.mixin.Shadow public net.minecraft.server.level.ServerPlayer player;
    @Inject(method="teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V",at=@At("HEAD"))
    private void clinging$teleport(net.minecraft.world.entity.PositionMoveRotation target,java.util.Set<net.minecraft.world.entity.Relative> relatives,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){MovingSurface.teleported(player);}
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="handleMovePlayer")
    private void clinging$relative(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        if(player.level().getServer().isSameThread()){
            MovingSurface.carry(player);
            if(packet.hasPosition()){
                var raw=new net.minecraft.world.phys.Vec3(packet.getX(player.getX()),packet.getY(player.getY()),packet.getZ(player.getZ()));
                var adjusted=MovingSurface.resolveMovement(player,raw);
                if(!adjusted.equals(raw))packet=new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(adjusted,packet.getYRot(player.getYRot()),packet.getXRot(player.getXRot()),packet.isOnGround(),packet.horizontalCollision());
            }
        }
        original.call(packet);
    }
    @Inject(method="noBlocksAround",at=@At("HEAD"),cancellable=true)
    private void clinging$support(Entity entity,CallbackInfoReturnable<Boolean> cir){
        if(entity instanceof net.minecraft.world.entity.LivingEntity living && !(entity instanceof Player) && MobGravity.active(living)
            && com.moigferdsrte.gravitychanger.util.GravityDirectionUtil.getGravityDirection(entity)!=net.minecraft.core.Direction.DOWN){cir.setReturnValue(false);return;}
        // Vanilla's floating check measures world Y. Falling sideways/upwards is not
        // flight; retain normal collision and movement-distance validation elsewhere.
        if(entity instanceof Player p && ClingingReoriented.controlsPhysics(p) && ClingingReoriented.data(p).selected!=net.minecraft.core.Direction.DOWN) {cir.setReturnValue(false);return;}
        if(entity instanceof Player p && ClingingReoriented.managed(p) && ClingingReoriented.data(p).groundedOnSurface) cir.setReturnValue(false);
    }
}
