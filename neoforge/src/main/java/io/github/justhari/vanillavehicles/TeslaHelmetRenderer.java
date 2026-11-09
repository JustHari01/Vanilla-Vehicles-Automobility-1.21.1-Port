package io.github.justhari.vanillavehicles;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Original Tesla head geometry/UV, attached to Java's armour head bone. */
public class TeslaHelmetRenderer extends GeoArmorRenderer<TeslaHelmetItem> {
    public TeslaHelmetRenderer(){super(new GeoModel<>(){
        @Override public ResourceLocation getModelResource(TeslaHelmetItem item){return VanillaVehicles.rl("geo/tesla_helmet.geo.json");}
        @Override public ResourceLocation getTextureResource(TeslaHelmetItem item){return VanillaVehicles.rl("textures/bw/vv/attachable/tesla_helmet.png");}
        @Override public ResourceLocation getAnimationResource(TeslaHelmetItem item){return VanillaVehicles.rl("animations/vehicle.animation.json");}
    });}
    @Override public GeoBone getHeadBone(GeoModel<TeslaHelmetItem> model){return model.getBone("head").orElse(null);}
    public static void createProvider(Consumer<GeoRenderProvider> consumer){consumer.accept(new GeoRenderProvider(){
        private TeslaHelmetRenderer renderer;
        @Override public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<T> original){
            if(renderer==null)renderer=new TeslaHelmetRenderer();renderer.prepForRender(entity,stack,slot,original);return renderer;
        }
    });}
}
