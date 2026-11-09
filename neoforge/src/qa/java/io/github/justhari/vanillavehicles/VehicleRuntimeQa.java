package io.github.justhari.vanillavehicles;

import java.util.*;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Development-only integration checks running in an actual NeoForge server world. */
@EventBusSubscriber(modid=VanillaVehicles.MOD_ID)
public class VehicleRuntimeQa {
    private static MinecraftServer server;
    private static ServerLevel level;
    private static int tick;
    private static final Map<VehicleKind,VehicleEntity> vehicles=new EnumMap<>(VehicleKind.class);
    private static final Map<VehicleKind,net.minecraft.server.level.ServerPlayer> riders=new EnumMap<>(VehicleKind.class);
    private static final Map<VehicleKind,Double> initialZ=new EnumMap<>(VehicleKind.class);
    private static boolean failed;
    private static int observedProjectiles;
    private static VehicleEntity boostProbe;
    private static UtilityEntity compressor;
    private static MechanicEntity mechanic;
    private static io.github.foundationgames.automobility.entity.AutomobileEntity baseCar;
    @SubscribeEvent public static void projectile(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(Boolean.getBoolean("vv.qa")&&event.getEntity() instanceof VehicleProjectile)observedProjectiles++;
    }
    private static net.minecraft.server.level.ServerPlayer player(String name){
        var player=new net.minecraft.server.level.ServerPlayer(server,level,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()),name),net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static void check(boolean condition,String message){if(!condition){failed=true;LogUtils.getLogger().error("[VV QA] FAIL {}",message);}else LogUtils.getLogger().info("[VV QA] PASS {}",message);}
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!Boolean.getBoolean("vv.qa"))return;
        server=event.getServer();level=server.overworld();
        try{
            for(int x=-10;x<100;x++)for(int z=-144;z<90;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),2);
            int i=0;
            for(var kind:VehicleKind.values()){
                int x=i++*10;
                var v=VanillaVehicles.VEHICLES.get(kind).get().create(level);v.setPos(x+.5,80,.5);v.setYRot(0);v.setFuel(10);
                if(kind==VehicleKind.BALLOON){v.setVariant("envelope","blank");v.setFlag("heated",true);}
                if(kind==VehicleKind.STEAMBOAT||kind==VehicleKind.PRISMER){
                    for(int wx=x-3;wx<=x+3;wx++)for(int wz=-3;wz<80;wz++)for(int y=80;y<84;y++)level.setBlock(new BlockPos(wx,y,wz),Blocks.WATER.defaultBlockState(),2);
                    v.setPos(x+.5,81,.5);
                }
                level.addFreshEntity(v);vehicles.put(kind,v);initialZ.put(kind,v.getZ());
                var player=player("VV_"+kind.id);
                player.setPos(v.getX(),v.getY()+2,v.getZ());player.setYRot(0);player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(VanillaVehicles.item("tesla_helmet")));
                check(player.startRiding(v),kind.id+" mounts");riders.put(kind,player);
                check(v.getControllingPassenger()==player,kind.id+" controlling rider");
                check(v.getType().builtInRegistryHolder().key().location().getNamespace().equals("bw_vv"),kind.id+" entity registry");
                v.inventory.setItem(0,new ItemStack(Items.DIAMOND,3));v.setVariant("sticker","creeper");v.setColor(14);v.setFlag("chest",true);
                CompoundTag saved=new CompoundTag();v.save(saved);
                var restored=VanillaVehicles.VEHICLES.get(kind).get().create(level);restored.load(saved);
                check(restored.fuel()==10&&restored.color()==14&&restored.variant("sticker").equals("creeper")&&restored.inventory.getItem(0).getCount()==3,kind.id+" NBT customization fuel inventory round trip");
                // This detached probe has the live vehicle's UUID. Calling discard() on
                // it can unregister the real entity from the server's UUID lookup.
                check(level.getEntity(v.getUUID())==v,kind.id+" remains registered after detached NBT probe");
                float before=v.fuel();v.acceptInput(new VehicleControls(Float.NaN,0,0,0,false,false));check(v.fuel()==before,kind.id+" rejects non-finite input");
            }
            LogUtils.getLogger().info("[VV QA] Starting 6000 server tick stability scenario");
            check(vehicles.get(VehicleKind.BALLOON).inventory.getContainerSize()==4&&vehicles.get(VehicleKind.STEAMBOAT).inventory.getContainerSize()==4,"bundle storage retains four slots");
            mechanic=VanillaVehicles.MECHANIC.get().create(level);mechanic.setPos(6.5,80,-5.5);mechanic.setNoAi(true);mechanic.setVillagerXp(60);level.addFreshEntity(mechanic);mechanic.getOffers();mechanic.updateTrades();
            check(mechanic.getOffers().stream().anyMatch(offer->offer.getResult().is(VanillaVehicles.item("blueprints"))),"Mechanic progression unlocks blueprint trade");
            check(mechanic.getOffers().stream().anyMatch(offer->offer.getResult().is(VanillaVehicles.item("tesla_helmet_upgrade_smithing_template"))),"Mechanic progression unlocks Tesla template trade");
            level.setBlock(new BlockPos(3,80,-5),VanillaVehicles.WIND_VANE.get().defaultBlockState(),3);
            check(level.getBlockEntity(new BlockPos(3,80,-5)) instanceof WindVane.VaneEntity,"wind vane block entity registered");
            var state=level.getBlockState(new BlockPos(3,80,-5));int powered=0;for(var dir:net.minecraft.core.Direction.values())if(state.getSignal(level,new BlockPos(3,80,-5),dir)==15)powered++;
            check(powered==1,"wind vane emits redstone power 15 in exactly one direction");
            level.setBlock(new BlockPos(4,80,-5),VanillaVehicles.FLAGS.get("flag.red").get().defaultBlockState(),3);
            check(level.getBlockEntity(new BlockPos(4,80,-5)) instanceof FlagBlock.FlagEntity,"original decorative flags place as wind-aware blocks");
            baseCar=io.github.foundationgames.automobility.entity.AutomobilityEntities.AUTOMOBILE.get().orElseThrow().create(level);baseCar.setPos(8.5,80,-8.5);level.addFreshEntity(baseCar);
            check(baseCar!=null,"Automobility automobile still registers beside all eight addon types");
            verifyRecipes();verifyInteractions();
        }catch(Throwable error){failed=true;LogUtils.getLogger().error("[VV QA] Setup failed",error);server.halt(false);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(server==null||server!=event.getServer()||server.isStopped())return;
        tick++;
        try{
            for(var entry:vehicles.entrySet()){
                var kind=entry.getKey();var v=entry.getValue();
                var chunk=v.chunkPosition();for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.setChunkForced(chunk.x+dx,chunk.z+dz,true);
                float pitch=kind==VehicleKind.PLANE&&tick<100?-18:0;
                boolean jump=kind==VehicleKind.BALLOON&&tick<400||kind==VehicleKind.MECH&&tick%160<30;
                boolean attack=kind==VehicleKind.MECH&&tick%100==20;
                v.acceptInput(new VehicleControls(tick<120?1:0,0,0,pitch,jump,attack));
                checkFinite(v);
            }
            if(tick==100){
                for(var entry:vehicles.entrySet()){
                    var kind=entry.getKey();var v=entry.getValue();
                    check(kind==VehicleKind.BALLOON? v.getY()>81 : v.getZ()>initialZ.get(kind)+.5,kind.id+" moves in actual world");
                }
                check(vehicles.get(VehicleKind.PLANE).getY()>81,"plane takes off");
                check(riders.get(VehicleKind.PRISMER).hasEffect(net.minecraft.world.effect.MobEffects.CONDUIT_POWER),"Prismer applies conduit power underwater");
                check(observedProjectiles>0,"mech spawns real projectile (world entity-join event)");
            }
            if(tick==400){
                vehicles.get(VehicleKind.BALLOON).setFlag("heated",false);
                // The movement assertions above exercise each real drive mode. Park the
                // fixtures before the long soak so wind and repeated QA jumps don't
                // carry test entities away from the chunks whose saves are checked.
                int slot=0;for(var kind:VehicleKind.values()){
                    var v=vehicles.get(kind);v.setPos(slot++*10+.5,80,.5);v.setYRot(0);v.setXRot(0);v.setDeltaMovement(0,0,0);
                    if(kind==VehicleKind.MECH)riders.get(kind).setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                }
            }
            if(tick==300){
                var car=vehicles.get(VehicleKind.LOGCAR);
                var p2=player("VV_Second");check(p2.startRiding(car),"logcar second player seat");
                var p3=player("VV_Third");check(!p3.startRiding(car),"logcar rejects third player");
                var truck=vehicles.get(VehicleKind.MOBTRUCK);var cow=EntityType.COW.create(level);cow.setPos(truck.getX(),truck.getY(),truck.getZ());level.addFreshEntity(cow);
                truck.setFlag("trunk_open",true);check(cow.startRiding(truck),"mobtruck accepts mob cargo");
                check(!vehicles.get(VehicleKind.MINECAR).canAddPassenger(cow),"minecar rejects mob cargo");
                var driver=riders.get(VehicleKind.MOBTRUCK);driver.stopRiding();check(driver.startRiding(truck)&&truck.getControllingPassenger()==driver,"mob cargo loaded first does not steal driver control");
            }
            if(tick==100){CompoundTag boost=new CompoundTag();boostProbe.save(boost);check(boost.getInt("BoostTicks")==0&&boost.getInt("BoostCooldown")==140,"boost lasts exactly five seconds before seven-second cooldown");}
            if(tick==240)check(boostProbe.activateBoost(),"boost becomes available after full cooldown");
            if(tick==6000){
                check(vehicles.values().stream().allMatch(v->!v.isRemoved()),"all eight vehicles survive 6000 ticks");
                check(vehicles.values().stream().allMatch(v->v.getY()>-48&&v.getY()<350&&Math.abs(v.getX())<256&&Math.abs(v.getZ())<256),"all vehicles remain inside saved test-world bounds");
                check(riders.values().stream().allMatch(net.minecraft.server.level.ServerPlayer::isAlive),"riders survive power jumps and vehicle travel");
                for(var v:vehicles.values())check(v.tickCount>=5900,v.kind.id+" actually ticked throughout stability scenario ("+v.tickCount+")");
                for(var v:vehicles.values())check(level.getEntity(v.getUUID())==v,v.kind.id+" UUID remains registered at save time");
                check(!baseCar.isRemoved()&&baseCar.tickCount>=5900,"base Automobility survives alongside addon vehicles");
                check(mechanic.isAlive(),"Mechanic survives alongside vehicle simulation");
                // Vanilla saves an occupied vehicle with exactly one player as part
                // of that player's RootVehicle data instead of the chunk entity list.
                // These QA ServerPlayers are not in PlayerList/playerdata, so detach
                // them only after the rider-survival assertions to exercise chunk save.
                for(var v:vehicles.values()){
                    v.ejectPassengers();
                    check(v.shouldBeSaved(),v.kind.id+" is eligible for chunk persistence after QA riders are detached");
                }
                writePersistenceProof();
                LogUtils.getLogger().info("[VV QA] Mean tick time {} ms",server.getAverageTickTimeNanos()/1_000_000.0);
                LogUtils.getLogger().info("[VV QA] RESULT {}",failed?"FAILED":"PASSED");server.halt(false);
            }
        }catch(Throwable error){failed=true;LogUtils.getLogger().error("[VV QA] Runtime exception at tick "+tick,error);server.halt(false);}
    }
    private static void checkFinite(VehicleEntity v){if(!Double.isFinite(v.getX())||!Double.isFinite(v.getY())||!Double.isFinite(v.getZ()))throw new AssertionError("Non-finite position "+v.kind.id);}
    private static void verifyRecipes(){
        for(var raw:VanillaVehicles.CATALOG.getAsJsonArray("recipes")){
            String name=raw.getAsString();var holder=server.getRecipeManager().byKey(VanillaVehicles.rl(name));
            check(holder.isPresent(),"recipe loads "+name);if(holder.isEmpty())continue;
            var recipe=holder.get().value();
            if(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting){
                var ingredients=crafting.getIngredients();var items=new ArrayList<ItemStack>();
                for(var ingredient:ingredients)items.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copy());
                int width=crafting instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped?shaped.getWidth():3;
                int height=crafting instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped?shaped.getHeight():3;
                while(items.size()<width*height)items.add(ItemStack.EMPTY);
                check(crafting.matches(net.minecraft.world.item.crafting.CraftingInput.of(width,height,items),level),"recipe crafts "+name);
            }else if(recipe instanceof net.minecraft.world.item.crafting.SmithingRecipe smithing){
                check(smithing.matches(new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(VanillaVehicles.item("tesla_helmet_upgrade_smithing_template")),new ItemStack(Items.NETHERITE_HELMET),new ItemStack(Items.NETHERITE_INGOT)),level),"Tesla helmet smithing recipe crafts");
            }
        }
    }
    private static void verifyInteractions(){
        var user=player("VV_Interactions");user.setPos(8.5,80,-5.5);var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        var car=VanillaVehicles.VEHICLES.get(VehicleKind.LOGCAR).get().create(level);car.setPos(8.5,80,-5.5);level.addFreshEntity(car);
        user.setItemInHand(hand,new ItemStack(Items.COAL,2));car.interact(user,hand);check(car.fuel()==1&&user.getItemInHand(hand).getCount()==1,"coal refuelling consumes one item and adds original fuel unit");
        user.setItemInHand(hand,new ItemStack(Items.CHEST));car.interact(user,hand);check(car.flag("chest")&&car.inventory.getContainerSize()==27,"logcar chest attachment enables 27 slots");
        check(car.fuel()==1,"chest attachment is not burnt as furnace fuel");
        user.setItemInHand(hand,new ItemStack(Items.WHITE_WOOL));car.interact(user,hand);check(car.flag("silent")&&car.fuel()==1,"wool silencer takes priority over furnace fuel");
        user.setItemInHand(hand,new ItemStack(Items.RED_DYE));car.interact(user,hand);check(car.color()==DyeColor.RED.getId(),"dye interaction stores original color");
        user.setItemInHand(hand,new ItemStack(VanillaVehicles.item("sticker.creeper")));car.interact(user,hand);check(car.variant("sticker").equals("creeper"),"sticker interaction preserves pattern");
        user.setItemInHand(hand,new ItemStack(Items.SHEARS));car.interact(user,hand);check(car.variant("sticker").isEmpty(),"shears remove sticker without deleting vehicle");
        float fuelBefore=car.fuel();user.setItemInHand(hand,new ItemStack(Items.OAK_PLANKS,2));car.interact(user,hand);
        check(car.fuel()==fuelBefore&&user.getItemInHand(hand).getCount()==2,"non-coal furnace fuel warns before consuming an item");car.interact(user,hand);
        check(car.fuel()>fuelBefore&&user.getItemInHand(hand).getCount()==1,"second confirmed click accepts other furnace fuel");
        user.setItemInHand(hand,new ItemStack(Items.COAL));car.interact(user,hand);boostProbe=car;check(car.activateBoost()&&!car.activateBoost(),"boost starts once and rejects repeated activation");
        compressor=VanillaVehicles.COMPRESSOR.get().create(level);compressor.setPos(7.5,80,-5.5);level.addFreshEntity(compressor);
        for(int i=0;i<256;i++){user.setItemInHand(hand,new ItemStack(Items.REDSTONE));compressor.interact(user,hand);}
        int count=0;for(int i=0;i<5;i++)count+=compressor.inventory.getItem(i).getCount();check(count==5*new ItemStack(VanillaVehicles.item("compressed_redstone")).getMaxStackSize(),"compressor fills its five original output slots ("+count+")");
        user.setItemInHand(hand,new ItemStack(Items.REDSTONE));compressor.interact(user,hand);check(user.getItemInHand(hand).getCount()==1,"full compressor does not delete extra redstone dust");
        CompoundTag saved=new CompoundTag();compressor.save(saved);var restored=VanillaVehicles.COMPRESSOR.get().create(level);restored.load(saved);int restoredCount=0;for(int i=0;i<5;i++)restoredCount+=restored.inventory.getItem(i).getCount();check(restoredCount==count,"compressor inventory survives NBT round trip");restored.discard();
        var boat=vehicles.get(VehicleKind.STEAMBOAT);user.setItemInHand(hand,new ItemStack(Items.BELL));boat.interact(user,hand);check(boat.flag("bell"),"ship accepts original bell attachment");
        var p2=player("VV_Boat2");var p3=player("VV_Boat3");var p4=player("VV_Boat4");check(p2.startRiding(boat)&&p3.startRiding(boat)&&!p4.startRiding(boat),"ship carries three players and rejects a fourth");
        var balloon=VanillaVehicles.VEHICLES.get(VehicleKind.BALLOON).get().create(level);balloon.setPos(9,80,-5);level.addFreshEntity(balloon);
        user.setItemInHand(hand,new ItemStack(VanillaVehicles.item("hot_air_balloon.envelope.creeper")));balloon.interact(user,hand);check(balloon.variant("envelope").equals("creeper"),"balloon accepts original envelope style");
        user.setItemInHand(hand,new ItemStack(Items.FLINT_AND_STEEL));balloon.interact(user,hand);check(balloon.flag("heated")&&balloon.fuel()==10,"balloon ignites and heats with flint and steel");
        user.setItemInHand(hand,new ItemStack(Items.SHEARS));balloon.interact(user,hand);check(balloon.variant("envelope").isEmpty()&&!balloon.flag("heated"),"balloon envelope can be removed safely");balloon.discard();
        check(new ItemStack(VanillaVehicles.item("tesla_helmet")).getMaxDamage()==500,"Tesla helmet retains original 500 durability");
    }
    private static void writePersistenceProof()throws java.io.IOException{
        var proof=new com.google.gson.JsonArray();
        for(var v:vehicles.values()){
            var row=new com.google.gson.JsonObject();row.addProperty("id",v.kind.id);row.addProperty("uuid",v.getUUID().toString());row.addProperty("x",v.getX());row.addProperty("y",v.getY());row.addProperty("z",v.getZ());row.addProperty("fuel",v.fuel());row.addProperty("color",v.color());row.addProperty("sticker",v.variant("sticker"));row.addProperty("diamonds",v.inventory.getItem(0).getCount());proof.add(row);
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("vv-persistence-proof.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(proof));
    }
}
