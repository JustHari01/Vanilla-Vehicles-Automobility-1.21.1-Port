package io.github.justhari.vanillavehicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public class UtilityEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Integer> COLOR=SynchedEntityData.defineId(UtilityEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WORK=SynchedEntityData.defineId(UtilityEntity.class,EntityDataSerializers.INT);
    public final boolean smoke;
    public final net.minecraft.world.SimpleContainer inventory=new net.minecraft.world.SimpleContainer(5);
    private int life=160;
    public UtilityEntity(EntityType<? extends UtilityEntity> type,Level level,boolean smoke){super(type,level);this.smoke=smoke;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(COLOR,0);builder.define(WORK,0);}
    public void setColor(DyeColor color){entityData.set(COLOR,color.getId());}
    public int color(){return entityData.get(COLOR);}
    public int workTicks(){return entityData.get(WORK);}
    @Override public boolean isPickable(){return !smoke;}
    @Override public boolean canBeCollidedWith(){return !smoke;}
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        if(!smoke&&player.getItemInHand(hand).is(Items.REDSTONE)){
            if(!level().isClientSide&&inventory.canAddItem(new ItemStack(VanillaVehicles.item("compressed_redstone")))){if(!player.getAbilities().instabuild)player.getItemInHand(hand).shrink(1);compress();}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(!smoke){if(!level().isClientSide)player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new VehicleStorageMenu(id,inv,this,inventory),net.minecraft.network.chat.Component.literal("Redstone Compressor")));return InteractionResult.sidedSuccess(level().isClientSide);}
        return super.interact(player,hand);
    }
    private void compress(){if(random.nextBoolean())inventory.addItem(new ItemStack(VanillaVehicles.item("compressed_redstone")));entityData.set(WORK,48);playSound(net.minecraft.sounds.SoundEvents.PISTON_EXTEND,.5f,1);}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide)return;
        if(workTicks()>0)entityData.set(WORK,workTicks()-1);
        if(smoke){
            if(--life<=0){spawnAtLocation(VanillaVehicles.item("smoke_bomb"));discard();return;}
            int rgb=DyeColor.byId(color()).getTextureDiffuseColor();
            ((net.minecraft.server.level.ServerLevel)level()).sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f),2),getX(),getY()+.4,getZ(),4,.25,.15,.25,.02);
        }else if(tickCount%6==0&&inventory.canAddItem(new ItemStack(VanillaVehicles.item("compressed_redstone")))){
            for(var dropped:level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(1.5),e->e.getItem().is(Items.REDSTONE))){dropped.getItem().shrink(1);if(dropped.getItem().isEmpty())dropped.discard();compress();break;}
        }
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        if(!level().isClientSide&&!smoke&&source.getEntity() instanceof Player){spawnAtLocation(VanillaVehicles.item("redstone_compressor"));for(int i=0;i<inventory.getContainerSize();i++)spawnAtLocation(inventory.removeItemNoUpdate(i));discard();}return true;
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){life=tag.getInt("Life");entityData.set(COLOR,tag.getInt("Color"));inventory.fromTag(tag.getList("Inventory",net.minecraft.nbt.Tag.TAG_COMPOUND),level().registryAccess());}
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putInt("Life",life);tag.putInt("Color",color());tag.put("Inventory",inventory.createTag(level().registryAccess()));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"base",0,state->state.setAndContinue(RawAnimation.begin().thenLoop("vehicle.idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
