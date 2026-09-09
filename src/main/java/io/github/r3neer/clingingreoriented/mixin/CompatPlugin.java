package io.github.r3neer.clingingreoriented.mixin;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import java.util.*;
public final class CompatPlugin implements IMixinConfigPlugin {
    public void onLoad(String p){} public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        if(mixin.endsWith("FirstPersonOffsetMixin"))return FabricLoader.getInstance().isModLoaded("firstperson");
        return !mixin.substring(mixin.lastIndexOf('.')+1).startsWith("Scale") || FabricLoader.getInstance().isModLoaded("scalebrews");
    }
    public void acceptTargets(Set<String>a,Set<String>b){} public List<String> getMixins(){return null;}
    public void preApply(String a,ClassNode b,String c,IMixinInfo d){} public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
