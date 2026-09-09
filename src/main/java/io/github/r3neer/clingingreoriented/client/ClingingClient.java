package io.github.r3neer.clingingreoriented.client;

import io.github.r3neer.clingingreoriented.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import java.util.*;

public final class ClingingClient implements ClientModInitializer {
    private static long sequence;
    private static final Set<Long> PENDING=new HashSet<>();
    private static final Map<Integer,Payloads.State> STATES=new HashMap<>();
    public static void press() {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.gui.screen()!=null || mc.gui.overlay()!=null || !mc.isWindowActive() || !ClingingReoriented.hasEffect(mc.player) || !ClientPlayNetworking.canSend(Payloads.Request.TYPE)) return;
        if(!GravityInput.available(mc.player))return;
        if(PENDING.size()>=16) return;
        // Snapshot the rendered view at the press, including upstream interpolation.
        // A separate movement packet may still contain the previous tick's yaw/pitch.
        var forward=mc.gameRenderer.mainCamera().rotation().transform(new org.joml.Vector3f(0,0,-1));
        var look=new net.minecraft.world.phys.Vec3(forward.x,forward.y,forward.z).normalize();
        long id=++sequence; PENDING.add(id); ClientPlayNetworking.send(new Payloads.Request(id,ClingingReoriented.data(mc.player).revision,look));
    }
    @Override public void onInitializeClient() {
        CameraConfig.load(CameraConfig.path());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client->BeaconPowers.install());
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Reply.TYPE,(reply,context)->{
            if(!PENDING.remove(reply.sequence()) || context.client().player==null) return;
            if(reply.result()==ClingingReoriented.Result.MOUNT_ACTION.ordinal())return;
            var sound=reply.result()==ClingingReoriented.Result.SUCCESS.ordinal()?SoundEvents.AMETHYST_BLOCK_CHIME:SoundEvents.NOTE_BLOCK_BASS.value();
            context.client().player.playSound(sound,.35f,reply.result()==0?1.25f:.8f);
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.State.TYPE,(state,context)->{
            if(state.direction()<0 || state.direction()>5) return;
            // Input callbacks can run before END_CLIENT_TICK after a teleport. Publish
            // the input epoch immediately, before queued geometry/binding application.
            if(context.client().level!=null && context.client().level.getEntity(state.player()) instanceof Player p && state.revision()>=ClingingReoriented.data(p).revision)ClingingReoriented.data(p).revision=state.revision();
            var previous=STATES.get(state.player());
            if(previous==null || state.revision()>=previous.revision()) STATES.put(state.player(),state);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{PENDING.clear();STATES.clear();sequence=0;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level==null) return;
            for(var it=STATES.entrySet().iterator();it.hasNext();) {
                var snapshot=it.next().getValue();
                if(!(client.level.getEntity(snapshot.player()) instanceof Player p)) continue;
                var s=ClingingReoriented.data(p);
                if(snapshot.revision()<s.revision) {it.remove();continue;}
                if(s.selected!=Direction.from3DDataValue(snapshot.direction()) && snapshot.owned()) {
                    p.setOnGround(false);p.verticalCollision=false;p.verticalCollisionBelow=false;p.horizontalCollision=false;
                }
                s.owned=snapshot.owned(); s.selected=Direction.from3DDataValue(snapshot.direction()); s.revision=snapshot.revision();
                if(com.moigferdsrte.gravitychanger.util.GravityDirectionUtil.getGravityDirection(p)==s.selected && s.owned)p.setBoundingBox(com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),s.selected,p.position()));
                if(snapshot.support()<0) {s.unbind();it.remove();}
                else if(client.level.getEntity(snapshot.support()) instanceof net.minecraft.world.entity.LivingEntity support && support.getUUID().equals(snapshot.supportUuid())) {MovingSurface.bind(p,support);it.remove();}
            }
            if(client.player!=null) MovingSurface.carry(client.player);
        });
    }
}
