package io.github.justhari.vanillavehicles;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UtilityRenderer extends GeoEntityRenderer<UtilityEntity>{
    public UtilityRenderer(EntityRendererProvider.Context ctx){super(ctx,new GeoModel<>(){
        @Override public ResourceLocation getModelResource(UtilityEntity e){return VanillaVehicles.rl("geo/"+(e.smoke?"smoke_bomb":"redstone_compressor")+".geo.json");}
        @Override public ResourceLocation getTextureResource(UtilityEntity e){return VanillaVehicles.rl(e.smoke?"textures/bw/vv/utilities/smoke_bomb/empty.png":"textures/bw/vv/utilities/redstone_compressor/animated/"+(1+(e.tickCount/5)%3)+".png");}
        @Override public ResourceLocation getAnimationResource(UtilityEntity e){return VanillaVehicles.rl("animations/vehicle.animation.json");}
        @Override public void setCustomAnimations(UtilityEntity e,long id,software.bernie.geckolib.animation.AnimationState<UtilityEntity> state){
            super.setCustomAnimations(e,id,state);
            if(e.smoke)return;float press=e.workTicks()>0?(float)Math.sin((48-e.workTicks()+state.getPartialTick())/48*Math.PI):0;
            getBone("bone2").ifPresent(b->b.setPosY(-press*3));getBone("bone4").ifPresent(b->b.setPosY(-press));
        }
    });}
}
