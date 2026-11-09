package io.github.justhari.vanillavehicles;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;

@Mod(VanillaVehicles.MOD_ID)
public class VanillaVehicles {
    public static final String MOD_ID = "vanilla_vehicles";
    public static final String NS = "bw_vv";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NS);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(NS);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,NS);
    public static final DeferredBlock<WindVane> WIND_VANE=BLOCKS.register("wind_vane",()->new WindVane(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(.8f).noOcclusion()));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<WindVane.VaneEntity>> VANE_ENTITY=BLOCK_ENTITIES.register("wind_vane",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(WindVane.VaneEntity::new,WIND_VANE.get()).build(null));
    public static final Map<String,DeferredBlock<FlagBlock>> FLAGS=new LinkedHashMap<>();
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<FlagBlock.FlagEntity>> FLAG_ENTITY=BLOCK_ENTITIES.register("flag",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(FlagBlock.FlagEntity::new,FLAGS.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NS);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT,NS);
    public static final Map<String,DeferredHolder<SoundEvent,SoundEvent>> SOUND_MAP = new HashMap<>();
    public static final Map<VehicleKind, DeferredHolder<EntityType<?>, EntityType<VehicleEntity>>> VEHICLES = new EnumMap<>(VehicleKind.class);
    public static final Map<String, DeferredItem<Item>> ITEM_MAP = new LinkedHashMap<>();
    public static final JsonObject CATALOG = readCatalog();
    public static final DeferredHolder<EntityType<?>,EntityType<VehicleProjectile>> PROJECTILE = ENTITIES.register("mech_projectile", () -> EntityType.Builder.<VehicleProjectile>of(VehicleProjectile::new,MobCategory.MISC).sized(.3f,.3f).clientTrackingRange(8).updateInterval(1).build(NS+":mech_projectile"));
    public static final DeferredHolder<EntityType<?>,EntityType<UtilityEntity>> COMPRESSOR=ENTITIES.register("redstone_compressor",()->EntityType.Builder.<UtilityEntity>of((type,level)->new UtilityEntity(type,level,false),MobCategory.MISC).sized(1,1).build(NS+":redstone_compressor"));
    public static final DeferredHolder<EntityType<?>,EntityType<UtilityEntity>> SMOKE=ENTITIES.register("smoke_bomb",()->EntityType.Builder.<UtilityEntity>of((type,level)->new UtilityEntity(type,level,true),MobCategory.MISC).sized(.4f,.4f).build(NS+":smoke_bomb"));
    public static final DeferredHolder<EntityType<?>,EntityType<MechanicEntity>> MECHANIC=ENTITIES.register("mechanic",()->EntityType.Builder.<MechanicEntity>of(MechanicEntity::new,MobCategory.CREATURE).sized(.6f,1.95f).build(NS+":mechanic"));
    static {
        for(var color:DyeColor.values()){String name="flag."+color.getName();FLAGS.put(name,BLOCKS.register(name,()->new FlagBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.5f).noOcclusion(),color.getName())));}
        FLAGS.put("flag.pole",BLOCKS.register("flag.pole",()->new FlagBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.5f).noOcclusion(),"pole")));
        try(var stream=VanillaVehicles.class.getResourceAsStream("/assets/bw_vv/sounds.json")){
            var soundData=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            for(String sound:soundData.keySet())SOUND_MAP.put(sound,SOUNDS.register(sound,()->SoundEvent.createVariableRangeEvent(rl(sound))));
        }catch(IOException e){throw new UncheckedIOException(e);}
        for (var kind : VehicleKind.values()) {
            VEHICLES.put(kind, ENTITIES.register(kind.id, () -> EntityType.Builder.<VehicleEntity>of((type,level) -> new VehicleEntity(type,level,kind), MobCategory.MISC)
                    .sized(kind.width,kind.height).clientTrackingRange(12).updateInterval(2).build(NS+":"+kind.id)));
        }
        for (var raw : CATALOG.getAsJsonArray("items")) {
            var entry = raw.getAsJsonObject();
            String id = entry.get("id").getAsString();
            int max = entry.get("stack").isJsonPrimitive() ? entry.get("stack").getAsInt() : entry.getAsJsonObject("stack").has("value") ? entry.getAsJsonObject("stack").get("value").getAsInt() : 64;
            String vehicle = entry.get("vehicle").isJsonNull() ? null : entry.get("vehicle").getAsString();
            ITEM_MAP.put(id, ITEMS.register(id, () -> id.equals("wind_vane")?new BlockItem(WIND_VANE.get(),new Item.Properties()):FLAGS.containsKey(id)?new BlockItem(FLAGS.get(id).get(),new Item.Properties()):id.equals("tesla_helmet") ? new TeslaHelmetItem() : new VehicleItem(new Item.Properties().stacksTo(Math.max(1,Math.min(64,max))), id, vehicle)));
        }
        // Original pack supplies Prismer only through world acquisition, not a craft recipe.
        if (!ITEM_MAP.containsKey("prismer")) ITEM_MAP.put("prismer", ITEMS.register("prismer", () -> new VehicleItem(new Item.Properties().stacksTo(1), "prismer", "prismer")));
        ITEMS.register("mechanic_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(MECHANIC,0x99734b,0x9cb5b0,new Item.Properties()));
        TABS.register("vehicles", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.bw_vv"))
                .icon(() -> new ItemStack(item("minecar"))).displayItems((params,output) -> ITEM_MAP.values().forEach(output::accept)).build());
    }
    public VanillaVehicles(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus); TABS.register(bus);SOUNDS.register(bus);BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);
        bus.addListener(VehicleControls::register);
        bus.addListener(VehicleSettings::register);
        bus.addListener(MechanicEntity::attributes);
    }
    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(NS,path); }
    public static Item item(String name) { var holder = ITEM_MAP.get(name); return holder == null ? Items.AIR : holder.get(); }
    public static SoundEvent sound(String name){var holder=SOUND_MAP.get(name);return holder==null?null:holder.get();}
    private static JsonObject readCatalog() {
        try (var input = VanillaVehicles.class.getResourceAsStream("/assets/bw_vv/catalog.json")) {
            if (input == null) throw new IllegalStateException("Missing vehicle catalog");
            return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
