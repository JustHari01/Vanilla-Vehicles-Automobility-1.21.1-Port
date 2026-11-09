package io.github.justhari.vanillavehicles;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;

public class MechanicRenderer extends GeoEntityRenderer<MechanicEntity>{
    public MechanicRenderer(EntityRendererProvider.Context ctx){super(ctx,new GeoModel<>(){
        @Override public ResourceLocation getModelResource(MechanicEntity e){return VanillaVehicles.rl("geo/mechanic.geo.json");}
        @Override public ResourceLocation getTextureResource(MechanicEntity e){return ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");}
        @Override public ResourceLocation getAnimationResource(MechanicEntity e){return VanillaVehicles.rl("animations/vehicle.animation.json");}
    });
        addRenderLayer(new GeoRenderLayer<>(this){@Override public void render(PoseStack pose,MechanicEntity e,BakedGeoModel model,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
            var texture=VanillaVehicles.rl("textures/bw/vv/entity/mechanic2/mechanic.png");var render=RenderType.entityCutoutNoCull(texture);
            getRenderer().reRender(model,pose,buffers,e,render,buffers.getBuffer(render),partial,light,overlay,0xffffffff);
        }});
    }
}
