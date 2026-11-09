package io.github.justhari.vanillavehicles;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-authoritative vehicle mechanics; client packets supply intent, never positions or fuel. */
public class VehicleEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(VehicleEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> CUSTOM = SynchedEntityData.defineId(VehicleEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(VehicleEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> MOTION = SynchedEntityData.defineId(VehicleEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final VehicleKind kind;
    public final SimpleContainer inventory;
    private VehicleControls input = new VehicleControls(0,0,0,0,false,false);
    private int inputTimeout, boostTicks, boostCooldown, weaponCooldown, smokeTicks, trickTicks, jumpCharge;
    private boolean jumpWasDown, attackWasDown, airborne;
    private float damageHits;
    private double speed, verticalSpeed;
    private double lerpX,lerpY,lerpZ;
    private float lerpYaw,lerpPitch;
    private int lerpTicks;
    private double renderDistance;
    private UUID fuelWarningPlayer;
    private ItemStack warnedFuel=ItemStack.EMPTY;
    private int fuelWarningUntil;

    public VehicleEntity(EntityType<? extends VehicleEntity> type,Level level,VehicleKind kind) {
        super(type,level); this.kind=kind;
        inventory=new SimpleContainer(kind==VehicleKind.STEAMBOAT||kind==VehicleKind.BALLOON?4:27);
        blocksBuilding=true;
        setFuel(kind.fuel == VehicleKind.Fuel.NONE ? 10 : 0);
    }
    @Override public float maxUpStep(){return 1f;}
    // Vanilla must not send stale client vehicle positions over the intent-only protocol.
    @Override public boolean isControlledByLocalInstance(){return false;}
    // Entity's default implementation forwards vehicle fall damage to every rider.
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUEL,0f); builder.define(CUSTOM,new CompoundTag()); builder.define(SPEED,0f);builder.define(MOTION,new CompoundTag());
    }
    public float fuel() { return entityData.get(FUEL); }
    public void setFuel(float amount) { entityData.set(FUEL,Mth.clamp(Float.isFinite(amount) ? amount : 0,0,10)); }
    public CompoundTag customization() { return entityData.get(CUSTOM).copy(); }
    public boolean flag(String key) { return entityData.get(CUSTOM).getBoolean(key); }
    public String variant(String key) { return entityData.get(CUSTOM).getString(key); }
    public int color() { return entityData.get(CUSTOM).contains("color") ? entityData.get(CUSTOM).getInt("color") : -1; }
    public float renderSpeed() { return entityData.get(SPEED); }
    public float roll(float partial) { int ticks=entityData.get(MOTION).getInt("roll");return ticks>0?(30-ticks+partial)/30f*360f:0; }
    public boolean airborne(){return entityData.get(MOTION).getBoolean("airborne");}
    public int boostCooldown(){return entityData.get(MOTION).getInt("boostCooldown");}
    public int weaponCooldown(){return entityData.get(MOTION).getInt("weaponCooldown");}
    public float steer(){return entityData.get(MOTION).getFloat("steer");}
    public double renderDistance(){return renderDistance;}
    public void setVariant(String key,String value) { var tag=customization();tag.putString(key,value);entityData.set(CUSTOM,tag); }
    public void setFlag(String key,boolean value) { var tag=customization();tag.putBoolean(key,value);entityData.set(CUSTOM,tag); }
    public void setColor(int value) { var tag=customization();tag.putInt("color",Mth.clamp(value,0,15));entityData.set(CUSTOM,tag); }
    public void acceptInput(VehicleControls raw) {
        if (!Float.isFinite(raw.forward()) || !Float.isFinite(raw.strafe()) || !Float.isFinite(raw.yaw()) || !Float.isFinite(raw.pitch())) return;
        input=new VehicleControls(Mth.clamp(raw.forward(),-1,1),Mth.clamp(raw.strafe(),-1,1),Mth.wrapDegrees(raw.yaw()),Mth.clamp(raw.pitch(),-90,90),raw.jump(),raw.attack());
        inputTimeout=10;
    }
    @Override public LivingEntity getControllingPassenger() { return getPassengers().stream().filter(p->p instanceof Player).map(p->(Player)p).findFirst().orElse(null); }
    @Override protected boolean canAddPassenger(Entity passenger) {
        if(kind==VehicleKind.MOBTRUCK&&passenger instanceof Player)return getPassengers().stream().noneMatch(p->p instanceof Player)&&getPassengers().size()<2;
        if (passenger instanceof Player) return getPassengers().size()<kind.seats;
        return kind==VehicleKind.MOBTRUCK && passenger instanceof Mob && getPassengers().size()<2 && flag("trunk_open");
    }
    @Override protected void positionRider(Entity passenger,MoveFunction move) {
        int seat=getPassengers().indexOf(passenger);
        double x=0, y=kind.seatY, z=kind.seatZ;
        if (kind==VehicleKind.LOGCAR && seat>0) { y=.5;z=-.45; }
        if (kind==VehicleKind.MOBTRUCK && !(passenger instanceof Player)) { y=1.1; z=1.15; }
        if (kind==VehicleKind.STEAMBOAT) {
            x=seat==0 ? .23 : seat==1 ? .49 : -.7;
            y=seat==0 ? 1 : .8; z=seat==0 ? -1.2 : 1.5;
        }
        Vec3 offset=new Vec3(x,y,z).yRot(-getYRot()*Mth.DEG_TO_RAD);
        move.accept(passenger,getX()+offset.x,getY()+offset.y,getZ()+offset.z);
    }
    @Override public boolean canBeCollidedWith() { return isAlive(); }
    @Override public boolean isPickable() { return isAlive(); }
    @Override public boolean isPushable() { return true; }
    @Override public boolean canCollideWith(Entity other) { return !isPassengerOfSameVehicle(other) && (other.canBeCollidedWith() || other.isPushable()); }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps) {
        lerpX=x;lerpY=y;lerpZ=z;lerpYaw=yaw;lerpPitch=pitch;lerpTicks=Math.max(1,steps);
    }
    @Override public void tick() {
        super.tick();
        for(var passenger:getPassengers())passenger.resetFallDistance();
        if (level().isClientSide) {
            Vec3 before=position();
            if (lerpTicks>0) {
                setPos(getX()+(lerpX-getX())/lerpTicks,getY()+(lerpY-getY())/lerpTicks,getZ()+(lerpZ-getZ())/lerpTicks);
                setYRot(getYRot()+Mth.wrapDegrees(lerpYaw-getYRot())/lerpTicks);setXRot(getXRot()+(lerpPitch-getXRot())/lerpTicks);lerpTicks--;
            }
            renderDistance+=position().subtract(before).horizontalDistance();
            return;
        }
        if (--inputTimeout<0 || getControllingPassenger()==null) input=new VehicleControls(0,0,getYRot(),getXRot(),false,false);
        if(boostCooldown>0&&boostTicks==0)boostCooldown--;if(weaponCooldown>0)weaponCooldown--;
        if(boostTicks>0)boostTicks--;if(smokeTicks>0)smokeTicks--;if(trickTicks>0)trickTicks--;
        boolean jumpStart=input.jump()&&!jumpWasDown, attackStart=input.attack()&&!attackWasDown;
        LivingEntity rider=getControllingPassenger();
        if(jumpStart && rider!=null && rider.getItemBySlot(EquipmentSlot.HEAD).is(VanillaVehicles.item("tesla_helmet"))) activateBoost();
        if(attackStart)primaryAction();
        if(kind==VehicleKind.MECH) {
            if(input.jump()&&onGround()) jumpCharge=Math.min(40,jumpCharge+1);
            if(!input.jump()&&jumpWasDown&&onGround()&&jumpCharge>0) { verticalSpeed=.42 + jumpCharge/40.0*.8;airborne=true; }
            if(!input.jump())jumpCharge=0;
        }
        drive();
        var motion=new CompoundTag();motion.putBoolean("airborne",airborne);motion.putInt("roll",trickTicks);motion.putInt("boostCooldown",boostCooldown);motion.putInt("weaponCooldown",weaponCooldown);
        motion.putFloat("steer",Mth.wrapDegrees(input.yaw()-getYRot()));motion.putFloat("strafe",input.strafe());entityData.set(MOTION,motion);
        jumpWasDown=input.jump();attackWasDown=input.attack();
        if(kind==VehicleKind.MOBTRUCK&&flag("trunk_open")&&getPassengers().size()<2) {
            for(var mob:level().getEntitiesOfClass(Mob.class,getBoundingBox().inflate(1.2),m->!m.isPassenger()&&m.getBbWidth()<1.9)) if(mob.startRiding(this))break;
        }
        if(kind==VehicleKind.PRISMER&&fuel()>0&&isInWater()&&rider!=null) {
            rider.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,40,9,false,false));
            rider.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,220,0,false,false));
        }
        if(smokeTicks>0&&level() instanceof ServerLevel server) {
            int rgb=DyeColor.byName(variant("smoke"),DyeColor.WHITE).getTextureDiffuseColor();
            server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f),2),getX(),getY()+.4,getZ(),3,.25,.15,.25,.02);
        }
        if(tickCount%10==0&&rider instanceof ServerPlayer player&&!player.getPersistentData().getBoolean("vv_hide_fuel_hud")) {
            String hud=kind==VehicleKind.MECH ? "Projectile "+Math.max(0,weaponCooldown/20.0)+"s | Boost "+Math.max(0,boostCooldown/20.0)+"s" : "Fuel "+Math.round(fuel()*10)+"%";
            player.displayClientMessage(Component.literal(hud),true);
        }
        if(tickCount%60==0&&fuel()>0&&kind!=VehicleKind.PRISMER&&kind!=VehicleKind.BALLOON){
            String sound="bw_vv.vehicle."+kind.id+".engine";
            if(Math.abs(speed)<.01)sound+=".standby";
            var event=VanillaVehicles.sound(sound);
            if(event!=null&&!flag("silent"))playSound(event,.35f,1f);
        }
    }
    private void drive() {
        boolean powered=kind.fuel==VehicleKind.Fuel.NONE||fuel()>0;
        double target=input.forward()*(powered?kind.speed:0);
        double gravity=onGround() ? 0 : -.08;
        double yawTarget=input.yaw();
        if(kind==VehicleKind.BALLOON) {
            boolean flying=flag("heated")&&!variant("envelope").isEmpty()&&powered;
            speed=Mth.approach((float)speed,flying ? .55f : 0,.002f);
            verticalSpeed=flying ? Mth.approach((float)verticalSpeed,input.jump()&&getControllingPassenger()!=null ? .13f : -.03f,.006f) : (verticalSpeed+gravity)*.98;
            // Deterministic world time wind: one cardinal change per 72,000 ticks, persisted by world time.
            int wind=WorldSystems.windIndex(level());
            Vec3 direction=switch(wind){case 0->new Vec3(0,0,-1);case 1->new Vec3(1,0,0);case 2->new Vec3(0,0,1);default->new Vec3(-1,0,0);};
            setDeltaMovement(direction.scale(speed).add(0,verticalSpeed,0));
            if(flying&&tickCount%20==0)setFuel(fuel()-kind.fuelUse);
        } else if(kind==VehicleKind.PLANE) {
            if(!airborne && onGround() && powered && input.forward()>0 && speed>.14 && input.pitch()<-8) {airborne=true;verticalSpeed=.12;}
            if(onGround()&&airborne&&verticalSpeed<=0)airborne=false;
            double max=airborne ? 1.1 : .33;
            // Keep forward flight speed proportional to rider input so releasing the
            // throttle slows an airborne plane instead of forcing it to cruise away.
            target=powered ? input.forward()*max : 0;
            speed=Mth.approach((float)speed,(float)target,.007f);
            setYRot(getYRot()+Mth.wrapDegrees((float)yawTarget-getYRot())/15);
            if(airborne) {
                verticalSpeed=powered ? Mth.lerp(.05,verticalSpeed,-Math.sin(input.pitch()*Mth.DEG_TO_RAD)*.6) : Math.max(-1.2,verticalSpeed-.05);
                setXRot(Mth.lerp(.08f,getXRot(),input.pitch()));
            } else verticalSpeed=(verticalSpeed+gravity)*.98;
            Vec3 forward=Vec3.directionFromRotation(0,getYRot());
            setDeltaMovement(forward.scale(speed*(boostTicks>0?1.6:1)).add(0,verticalSpeed,0));
        } else if(kind==VehicleKind.STEAMBOAT) {
            setYRot(getYRot()-input.strafe()*8);
            speed=Mth.approach((float)speed,(float)target,.006f);
            if(isInWater())verticalSpeed=Mth.clamp((float)(getDeltaMovement().y*.7+.04),-.04f,.06f);
            else verticalSpeed=(verticalSpeed+gravity)*.98;
            setDeltaMovement(Vec3.directionFromRotation(0,getYRot()).scale(speed).add(0,verticalSpeed,0));
        } else if(kind==VehicleKind.PRISMER) {
            setYRot(getYRot()+Mth.wrapDegrees((float)yawTarget-getYRot())/6);
            speed=Mth.approach((float)speed,isInWater()?(float)target:0,.01f);
            verticalSpeed=isInWater() ? (powered?-Math.sin(input.pitch()*Mth.DEG_TO_RAD)*Math.abs(speed):-.03) : (verticalSpeed-.14)*.98;
            setDeltaMovement(Vec3.directionFromRotation(0,getYRot()).scale(speed).add(0,verticalSpeed,0));
        } else {
            setYRot(getYRot()+Mth.wrapDegrees((float)yawTarget-getYRot())/6);
            speed=Mth.approach((float)speed,(float)target,.015f);
            verticalSpeed=(verticalSpeed+(kind==VehicleKind.MECH&&boostTicks>0&&verticalSpeed<0?-.01:gravity))*.98;
            if(horizontalCollision&&onGround()&&speed>0)verticalSpeed=.2;
            Vec3 direction=Vec3.directionFromRotation(0,getYRot());
            double boost=boostTicks>0 ? (kind==VehicleKind.MECH?1.0:.15) : 0;
            setDeltaMovement(direction.scale(speed+boost).add(0,verticalSpeed,0));
        }
        double impactSpeed=getDeltaMovement().horizontalDistance();
        boolean wasGround=onGround();
        move(MoverType.SELF,getDeltaMovement());
        if(onGround())verticalSpeed=0;
        if(kind==VehicleKind.PLANE&&airborne&&horizontalCollision&&impactSpeed>.55) {
            level().explode(this,getX(),getY(),getZ(),2.5f,Level.ExplosionInteraction.MOB);dropAndRemove();return;
        }
        if(kind==VehicleKind.MECH&&airborne&&!wasGround&&onGround()) {
            for(var living:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(4),e->!isPassengerOfSameVehicle(e))) {
                if(living.distanceToSqr(this)>16)continue;
                living.hurt(damageSources().explosion(this,getControllingPassenger()),15);
                Vec3 away=living.position().subtract(position()).multiply(1,0,1).normalize();living.push(away.x*2,.5,away.z*2);
            }
            airborne=false;
        }
        if(powered&&Math.abs(speed)>.01&&tickCount%20==0&&kind!=VehicleKind.BALLOON)setFuel(fuel()-kind.fuelUse);
        if(boostTicks>0&&kind.fuel!=VehicleKind.Fuel.NONE)setFuel(fuel()-.0023f);
        entityData.set(SPEED,(float)speed);
        if(isInWater()&&kind!=VehicleKind.STEAMBOAT&&kind!=VehicleKind.PRISMER&&kind!=VehicleKind.BALLOON)speed*=.95;
    }
    public boolean activateBoost() {
        if(boostCooldown>0||fuel()<=0||kind==VehicleKind.BALLOON||kind==VehicleKind.PRISMER||kind==VehicleKind.STEAMBOAT)return false;
        if(kind==VehicleKind.MECH&&onGround())return false;
        boostTicks=100;boostCooldown=140;var sound=VanillaVehicles.sound("bw_vv.vehicle.boost_start");if(sound!=null)playSound(sound,.5f,1);return true;
    }
    public void primaryAction() {
        if(kind==VehicleKind.PLANE&&airborne&&trickTicks==0){trickTicks=30;return;}
        if(kind==VehicleKind.STEAMBOAT&&flag("bell")){var sound=VanillaVehicles.sound("bw_vv.vehicle.steamboat.bell");playSound(sound==null?SoundEvents.BELL_BLOCK:sound,.8f,1);return;}
        if(kind!=VehicleKind.MECH||weaponCooldown>0||!(level() instanceof ServerLevel server))return;
        LivingEntity rider=getControllingPassenger();
        Vec3 direction=rider==null?Vec3.directionFromRotation(0,getYRot()):rider.getLookAngle();
        var projectile=new VehicleProjectile(VanillaVehicles.PROJECTILE.get(),server);
        projectile.setOwner(rider);projectile.setPos(getX()+direction.x*1.2,getY()+1.7,getZ()+direction.z*1.2);
        projectile.shoot(direction.x,direction.y,direction.z,1.5f,0);server.addFreshEntity(projectile);
        var sound=VanillaVehicles.sound("bw_vv.vehicle.mech.weapon");if(sound!=null)playSound(sound,.7f,1);
        weaponCooldown=60;
    }
    @Override public InteractionResult interact(Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(level().isClientSide)return InteractionResult.SUCCESS;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if(kind==VehicleKind.MOBTRUCK&&player.isShiftKeyDown()&&stack.isEmpty()) {
            boolean open=!flag("trunk_open");setFlag("trunk_open",open);
            if(open)for(var passenger:List.copyOf(getPassengers()))if(!(passenger instanceof Player))passenger.stopRiding();
            return InteractionResult.CONSUME;
        }
        if(stack.getItem() instanceof DyeItem dye) {setColor(dye.getDyeColor().getId());consume(player,stack);return InteractionResult.CONSUME;}
        if(id.startsWith("sticker.")){setVariant("sticker",id.substring(8));consume(player,stack);return InteractionResult.CONSUME;}
        if(stack.is(Items.SHEARS)) {
            if(kind==VehicleKind.BALLOON&&!variant("envelope").isEmpty()){spawnAtLocation(VanillaVehicles.item("hot_air_balloon.envelope."+variant("envelope")));setVariant("envelope","");setFlag("heated",false);}
            else {if(!variant("sticker").isEmpty())spawnAtLocation(VanillaVehicles.item("sticker."+variant("sticker")));setVariant("sticker","");}
            return InteractionResult.CONSUME;
        }
        if(id.startsWith("hot_air_balloon.envelope.")&&kind==VehicleKind.BALLOON){setVariant("envelope",id.substring("hot_air_balloon.envelope.".length()));consume(player,stack);return InteractionResult.CONSUME;}
        if(kind==VehicleKind.BALLOON&&stack.is(Items.FLINT_AND_STEEL)&&!variant("envelope").isEmpty()){setFlag("heated",true);setFuel(10);stack.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));return InteractionResult.CONSUME;}
        if(kind==VehicleKind.PRISMER&&stack.is(Items.CONDUIT)&&isInWater()&&fuel()==0){setFuel(9);consume(player,stack);return InteractionResult.CONSUME;}
        if(kind.fuel==VehicleKind.Fuel.REDSTONE&&stack.is(VanillaVehicles.item("compressed_redstone"))&&fuel()<10){setFuel(fuel()+1);consume(player,stack);return InteractionResult.CONSUME;}
        // Attachments and silencers are handled before the generic furnace-fuel
        // fallback: chests and wool are also valid furnace fuels in Java.
        boolean bundle=kind==VehicleKind.BALLOON||kind==VehicleKind.STEAMBOAT;
        if((bundle&&stack.is(Items.BUNDLE)||!bundle&&stack.is(Items.CHEST))&&!flag("chest")&&kind!=VehicleKind.MINECAR&&kind!=VehicleKind.PLANE&&kind!=VehicleKind.PRISMER&&kind!=VehicleKind.MOBTRUCK) {setFlag("chest",true);consume(player,stack);return InteractionResult.CONSUME;}
        if(stack.is(ItemTags.WOOL)){setFlag("silent",!flag("silent"));consume(player,stack);return InteractionResult.CONSUME;}
        if(kind.fuel==VehicleKind.Fuel.COAL&&fuel()<10) {
            int burn=stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
            if(burn>0){
                if(!stack.is(Items.COAL)&&!stack.is(Items.CHARCOAL)&&!stack.is(Items.COAL_BLOCK)&&!(player.getUUID().equals(fuelWarningPlayer)&&tickCount<=fuelWarningUntil&&ItemStack.isSameItemSameComponents(stack,warnedFuel))){
                    fuelWarningPlayer=player.getUUID();warnedFuel=stack.copyWithCount(1);fuelWarningUntil=tickCount+100;
                    player.displayClientMessage(Component.literal("Use "+stack.getHoverName().getString()+" as fuel? Right-click again within five seconds."),true);return InteractionResult.CONSUME;
                }
                setFuel(fuel()+burn/1600f);consume(player,stack);fuelWarningUntil=0;return InteractionResult.CONSUME;
            }
        }
        if(id.startsWith("smoke_bomb.")&&smokeTicks==0){setVariant("smoke",id.substring(11));smokeTicks=160;consume(player,stack);player.addItem(new ItemStack(VanillaVehicles.item("smoke_bomb")));return InteractionResult.CONSUME;}
        if(kind==VehicleKind.STEAMBOAT&&stack.is(Items.BELL)&&!flag("bell")){setFlag("bell",true);consume(player,stack);return InteractionResult.CONSUME;}
        if(player.isShiftKeyDown()&&flag("chest")) {
            player.openMenu(new SimpleMenuProvider((idMenu,inv,p)->new VehicleStorageMenu(idMenu,inv,this),Component.literal("Vehicle storage")));return InteractionResult.CONSUME;
        }
        if(player.startRiding(this))return InteractionResult.CONSUME;
        return InteractionResult.PASS;
    }
    @Override public InteractionResult interactAt(Player player,Vec3 hit,InteractionHand hand){
        Vec3 local=hit.yRot(getYRot()*Mth.DEG_TO_RAD);
        if(kind==VehicleKind.MOBTRUCK&&local.z>.4&&player.getItemInHand(hand).isEmpty()){
            if(!level().isClientSide){boolean open=!flag("trunk_open");setFlag("trunk_open",open);if(open)for(var cargo:List.copyOf(getPassengers()))if(!(cargo instanceof Player))cargo.stopRiding();}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }
    private static void consume(Player player,ItemStack stack){if(!player.getAbilities().instabuild)stack.shrink(1);}
    @Override public boolean hurt(DamageSource source,float amount) {
        if(level().isClientSide||isRemoved())return false;
        if(source.getEntity() instanceof Player p&&p.getVehicle()==this){primaryAction();return false;}
        Player player=source.getEntity() instanceof Player p?p:null;
        if(player==null)return false;
        boolean tool=player!=null&&(player.getMainHandItem().is(kind==VehicleKind.LOGCAR||kind==VehicleKind.PLANE?ItemTags.AXES:ItemTags.PICKAXES));
        var stats=VanillaVehicles.CATALOG.getAsJsonObject("vehicles").getAsJsonObject(kind.id);
        float resistance=stats.get("hitResistance").getAsFloat(),tools=stats.get("toolResistance").getAsFloat();
        damageHits+=tool?resistance/tools:1;
        if(player.getAbilities().instabuild||damageHits+.001>=resistance)dropAndRemove();
        return true;
    }
    public void dropAndRemove() {
        if(isRemoved())return;
        if(level() instanceof ServerLevel server){
            String item=kind==VehicleKind.BALLOON?"hot_air_balloon.basket":kind.id+(kind==VehicleKind.LOGCAR||kind==VehicleKind.PLANE?"."+(variant("wood").isEmpty()?"oak":variant("wood")):"");
            spawnAtLocation(VanillaVehicles.item(item));
            if(flag("chest"))spawnAtLocation(kind==VehicleKind.STEAMBOAT||kind==VehicleKind.BALLOON?Items.BUNDLE:Items.CHEST);
            for(int i=0;i<inventory.getContainerSize();i++)spawnAtLocation(inventory.removeItemNoUpdate(i));
            if(flag("bell"))spawnAtLocation(Items.BELL);
            if(!variant("envelope").isEmpty())spawnAtLocation(VanillaVehicles.item("hot_air_balloon.envelope."+variant("envelope")));
        }
        ejectPassengers();discard();
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        setFuel(tag.getFloat("Fuel"));entityData.set(CUSTOM,tag.getCompound("Customization"));damageHits=tag.getFloat("DamageHits");
        boostCooldown=Math.max(0,tag.getInt("BoostCooldown"));weaponCooldown=Math.max(0,tag.getInt("WeaponCooldown"));
        boostTicks=Mth.clamp(tag.getInt("BoostTicks"),0,100);smokeTicks=Mth.clamp(tag.getInt("SmokeTicks"),0,160);trickTicks=Mth.clamp(tag.getInt("TrickTicks"),0,30);
        inventory.fromTag(tag.getList("Inventory",Tag.TAG_COMPOUND),level().registryAccess());
        airborne=tag.getBoolean("Airborne");speed=tag.getDouble("Speed");verticalSpeed=tag.getDouble("VerticalSpeed");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Fuel",fuel());tag.put("Customization",customization());tag.putFloat("DamageHits",damageHits);
        tag.putInt("BoostCooldown",boostCooldown);tag.putInt("WeaponCooldown",weaponCooldown);tag.put("Inventory",inventory.createTag(level().registryAccess()));
        tag.putInt("BoostTicks",boostTicks);tag.putInt("SmokeTicks",smokeTicks);tag.putInt("TrickTicks",trickTicks);
        tag.putBoolean("Airborne",airborne);tag.putDouble("Speed",speed);tag.putDouble("VerticalSpeed",verticalSpeed);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"base",3,state->{
            String animation="vehicle.idle";
            if(kind==VehicleKind.MECH&&Math.abs(renderSpeed())>.01&&!airborne())animation="animation.bw_vv.mech_walk_"+(renderSpeed()<0?"backward":"forward");
            if(kind==VehicleKind.STEAMBOAT)animation="animation.bw_vv.steamboat_idle";
            if(kind==VehicleKind.PRISMER)animation="animation.bw_vv.prismer_"+(Math.abs(renderSpeed())>.01?"swimming":"standby");
            if(kind==VehicleKind.BALLOON)animation="animation.bw_vv.hot_air_balloon_"+(flag("heated")&&fuel()>0?"idle":"deflated");
            return state.setAndContinue(RawAnimation.begin().thenLoop(animation));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
