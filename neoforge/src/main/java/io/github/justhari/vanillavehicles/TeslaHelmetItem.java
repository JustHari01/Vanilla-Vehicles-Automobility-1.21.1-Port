package io.github.justhari.vanillavehicles;

import java.util.function.Consumer;
import net.minecraft.world.item.*;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TeslaHelmetItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public TeslaHelmetItem(){super(ArmorMaterials.NETHERITE,Type.HELMET,new Item.Properties().durability(500).fireResistant());}
    @Override public boolean isValidRepairItem(ItemStack armor,ItemStack material){return material.is(VanillaVehicles.item("compressed_redstone"));}
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer){TeslaHelmetRenderer.createProvider(consumer);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"base",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("vehicle.idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
