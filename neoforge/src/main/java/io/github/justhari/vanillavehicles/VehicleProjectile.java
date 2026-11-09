package io.github.justhari.vanillavehicles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class VehicleProjectile extends ThrowableItemProjectile implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    public VehicleProjectile(EntityType<? extends VehicleProjectile> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return VanillaVehicles.item("compressed_redstone");}
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);
        if(!level().isClientSide){
            if(hit instanceof net.minecraft.world.phys.EntityHitResult entityHit)entityHit.getEntity().hurt(damageSources().thrown(this,getOwner()),6);
            level().explode(this,getX(),getY(),getZ(),2f,Level.ExplosionInteraction.MOB);discard();
        }
    }
    @Override public void tick(){super.tick();if(tickCount>100&&!level().isClientSide)discard();}
    @Override protected double getDefaultGravity(){return .0001;}
    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity){return !(entity instanceof VehicleEntity)&&super.canHitEntity(entity);}
    @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers){controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this,"base",0,s->s.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("vehicle.idle"))));}
    @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
