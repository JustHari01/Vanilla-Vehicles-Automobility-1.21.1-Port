package io.github.foundationgames.automobility.automobile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.automobility.Automobility;
import io.github.foundationgames.automobility.util.AUtils;
import io.github.foundationgames.automobility.util.DefaultRegistrar;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

public record AutomobileFrame(
        boolean empty,
        float weight,
        float width,
        List<Hitbox> hitboxes,
        HornSoundDefinition horn,
        FrameModel model
) implements AutomobileComponent<AutomobileFrame> {
    private static final Map<AutomobileFrame, ResourceLocation> LEGACY_IDS = new IdentityHashMap<>();

    /** Compatibility constructor for addons written against Automobility 0.4.x. */
    public AutomobileFrame(ResourceLocation id, float weight, FrameModel model) {
        this(false, weight, 1f, List.of(), HornSoundDefinition.DEFAULT, model);
        LEGACY_IDS.put(this, id);
    }

    public static void rememberId(AutomobileFrame frame, ResourceLocation id) {
        LEGACY_IDS.put(frame, id);
    }
    public static final ResourceLocation ID = Automobility.rl("frame");

    public static final ResourceKey<Registry<AutomobileFrame>> REGISTRY = ResourceKey.createRegistryKey(Automobility.rl("automobile_frame"));
    public static final DefaultRegistrar<AutomobileFrame> BOOTSTRAP = new DefaultRegistrar<>(REGISTRY);

    public static final Codec<AutomobileFrame> DIRECT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("_empty", false).forGetter(AutomobileFrame::empty),
            Codec.FLOAT.fieldOf("weight").forGetter(AutomobileFrame::weight),
            Codec.FLOAT.optionalFieldOf("width", 1f).forGetter(AutomobileFrame::width),
            Codec.list(Hitbox.CODEC).optionalFieldOf("hitboxes", List.of()).forGetter(AutomobileFrame::hitboxes),
            HornSoundDefinition.CODEC.optionalFieldOf("horn", HornSoundDefinition.DEFAULT).forGetter(AutomobileFrame::horn),
            FrameModel.CODEC.fieldOf("display").forGetter(AutomobileFrame::model)
    ).apply(inst, AutomobileFrame::new));
    public static final Codec<ResourceKey<AutomobileFrame>> CODEC = ResourceKey.codec(REGISTRY);

    public static final StreamCodec<RegistryFriendlyByteBuf, AutomobileFrame> DIRECT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AutomobileFrame::empty,
            ByteBufCodecs.FLOAT, AutomobileFrame::weight,
            ByteBufCodecs.FLOAT, AutomobileFrame::width,
            ByteBufCodecs.<ByteBuf, Hitbox>list().apply(Hitbox.STREAM_CODEC), AutomobileFrame::hitboxes,
            HornSoundDefinition.STREAM_CODEC, AutomobileFrame::horn,
            FrameModel.STREAM_CODEC, AutomobileFrame::model,
            AutomobileFrame::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<AutomobileFrame>> STREAM_CODEC = ByteBufCodecs.holder(REGISTRY, DIRECT_STREAM_CODEC);
    public static final EntityDataSerializer<Holder<AutomobileFrame>> SERIALIZER = EntityDataSerializer.forValueType(STREAM_CODEC);

    public static final AutomobileFrame EMPTY = new AutomobileFrame(
            true,
            0.25f,
            1f,
            List.of(),
            HornSoundDefinition.DEFAULT,
            FrameModel.legacy(
                    ResourceLocation.parse("empty"),
                    Automobility.rl("empty"),
                    WheelBase.basic(16, 16),
                    16, 8, 8, 4, 8, 8
            )
    );

    public static final ResourceKey<AutomobileFrame> EMPTY_KEY = BOOTSTRAP.register(
            Automobility.rl("empty"), EMPTY
    );

    public static AutomobileFrame of(float weight, float width, List<Hitbox> hitboxes, HornSoundDefinition horn, FrameModel model) {
        return new AutomobileFrame(false, weight, width, hitboxes, horn, model);
    }

    public static AutomobileFrame of(float weight, List<Hitbox> hitboxes, HornSoundDefinition horn, FrameModel model) {
        return of(weight, 1f, hitboxes, horn, model);
    }

    public static AutomobileFrame get(ResourceKey<AutomobileFrame> key, HolderLookup.Provider registries) {
        return registries.lookupOrThrow(REGISTRY).get(key).map(Holder.Reference::value).orElse(EMPTY);
    }

    public EntityDimensions makeBounds() {
        return EntityDimensions.scalable(this.width(), 0.66f);
    }

    public static final ResourceKey<AutomobileFrame> WOODEN_MOTORCAR = BOOTSTRAP.register(motorcar("wooden", 0.3f, HornSoundDefinition.klaxon(11)));
    public static final ResourceKey<AutomobileFrame> COPPER_MOTORCAR = BOOTSTRAP.register(motorcar("copper", 0.4f, HornSoundDefinition.disc(20, 23)));
    public static final ResourceKey<AutomobileFrame> STEEL_MOTORCAR = BOOTSTRAP.register(motorcar("steel", 0.475f, HornSoundDefinition.brass(16, 19)));
    public static final ResourceKey<AutomobileFrame> GOLDEN_MOTORCAR = BOOTSTRAP.register(motorcar("golden", 0.525f, HornSoundDefinition.trumpet(14, 17)));
    public static final ResourceKey<AutomobileFrame> BEJEWELED_MOTORCAR = BOOTSTRAP.register(motorcar("bejeweled", 0.555f, HornSoundDefinition.trumpet(10, 13, 18)));

    public static final ResourceKey<AutomobileFrame> STANDARD_WHITE = BOOTSTRAP.register(standard("white", 1, 13));
    public static final ResourceKey<AutomobileFrame> STANDARD_ORANGE = BOOTSTRAP.register(standard("orange", 7, 8, 11));
    public static final ResourceKey<AutomobileFrame> STANDARD_MAGENTA = BOOTSTRAP.register(standard("magenta", 8, 10, 12));
    public static final ResourceKey<AutomobileFrame> STANDARD_LIGHT_BLUE = BOOTSTRAP.register(standard("light_blue", 11, 13, 17));
    public static final ResourceKey<AutomobileFrame> STANDARD_YELLOW = BOOTSTRAP.register(standard("yellow", 9, 12, 14));
    public static final ResourceKey<AutomobileFrame> STANDARD_LIME = BOOTSTRAP.register(standard("lime", 10, 14, 17, 19));
    public static final ResourceKey<AutomobileFrame> STANDARD_PINK = BOOTSTRAP.register(standard("pink", 5, 7, 12));
    public static final ResourceKey<AutomobileFrame> STANDARD_GRAY = BOOTSTRAP.register(standard("gray", 0, 6));
    public static final ResourceKey<AutomobileFrame> STANDARD_LIGHT_GRAY = BOOTSTRAP.register(standard("light_gray", 12, 18));
    public static final ResourceKey<AutomobileFrame> STANDARD_CYAN = BOOTSTRAP.register(standard("cyan", 8, 12, 15));
    public static final ResourceKey<AutomobileFrame> STANDARD_PURPLE = BOOTSTRAP.register(standard("purple", 4, 6, 11));
    public static final ResourceKey<AutomobileFrame> STANDARD_BLUE = BOOTSTRAP.register(standard("blue", 10, 13));
    public static final ResourceKey<AutomobileFrame> STANDARD_BROWN = BOOTSTRAP.register(standard("brown", 0, 4));
    public static final ResourceKey<AutomobileFrame> STANDARD_GREEN = BOOTSTRAP.register(standard("green", 7, 10, 13, 15));
    public static final ResourceKey<AutomobileFrame> STANDARD_RED = BOOTSTRAP.register(standard("red", 3, 6));
    public static final ResourceKey<AutomobileFrame> STANDARD_BLACK = BOOTSTRAP.register(standard("black", 1, 4, 7, 9));

    public static final ResourceKey<AutomobileFrame> AMETHYST_RICKSHAW = BOOTSTRAP.register(rickshaw("amethyst", 0.2f, HornSoundDefinition.klaxon(18)));
    public static final ResourceKey<AutomobileFrame> QUARTZ_RICKSHAW = BOOTSTRAP.register(rickshaw("quartz", 0.25f, HornSoundDefinition.trumpet(10, 13)));
    public static final ResourceKey<AutomobileFrame> PRISMARINE_RICKSHAW = BOOTSTRAP.register(rickshaw("prismarine", 0.14f, HornSoundDefinition.trumpet(18, 21)));
    public static final ResourceKey<AutomobileFrame> ECHO_RICKSHAW = BOOTSTRAP.register(rickshaw("echo", 0.1f, HornSoundDefinition.trumpet(8, 18)));

    public static final ResourceKey<AutomobileFrame> RED_TRACTOR = BOOTSTRAP.register(tractor("red", 13, 16));
    public static final ResourceKey<AutomobileFrame> YELLOW_TRACTOR = BOOTSTRAP.register(tractor("yellow", 9, 12));
    public static final ResourceKey<AutomobileFrame> GREEN_TRACTOR = BOOTSTRAP.register(tractor("green", 7, 13, 15));
    public static final ResourceKey<AutomobileFrame> BLUE_TRACTOR = BOOTSTRAP.register(tractor("blue", 10, 13));

    // Mavica frames. These are dynamic-registry entries in 1.21.1, so they
    // must be present here as well as in the generated datapack resources.
    public static final ResourceKey<AutomobileFrame> JEEP_RED = BOOTSTRAP.register(jeep("red"));
    public static final ResourceKey<AutomobileFrame> JEEP_ORANGE = BOOTSTRAP.register(jeep("orange"));
    public static final ResourceKey<AutomobileFrame> JEEP_YELLOW = BOOTSTRAP.register(jeep("yellow"));
    public static final ResourceKey<AutomobileFrame> JEEP_GREEN = BOOTSTRAP.register(jeep("green"));
    public static final ResourceKey<AutomobileFrame> JEEP_BLUE = BOOTSTRAP.register(jeep("blue"));
    public static final ResourceKey<AutomobileFrame> JEEP_INDIGO = BOOTSTRAP.register(jeep("indigo"));
    public static final ResourceKey<AutomobileFrame> JEEP_VIOLET = BOOTSTRAP.register(jeep("violet"));
    public static final ResourceKey<AutomobileFrame> JEEP_WHITE = BOOTSTRAP.register(jeep("white"));
    public static final ResourceKey<AutomobileFrame> JEEP_BLACK = BOOTSTRAP.register(jeep("black"));

    public static final ResourceKey<AutomobileFrame> BEETLE_PINK = BOOTSTRAP.register(beetle("pink"));
    public static final ResourceKey<AutomobileFrame> BEETLE_TEAL = BOOTSTRAP.register(beetle("teal"));
    public static final ResourceKey<AutomobileFrame> BEETLE_BLUE = BOOTSTRAP.register(beetle("blue"));
    public static final ResourceKey<AutomobileFrame> BEETLE_YELLOW = BOOTSTRAP.register(beetle("yellow"));
    public static final ResourceKey<AutomobileFrame> BEETLE_MAUVE = BOOTSTRAP.register(beetle("mauve"));
    public static final ResourceKey<AutomobileFrame> BEETLE_CREAM = BOOTSTRAP.register(beetle("cream", 0.55f));

    // Vanilla-friendly vehicle collection. Each entry reuses the existing
    // frame/wheel/engine/attachment pipeline; only the silhouette and texture
    // are new assets.
    public static final ResourceKey<AutomobileFrame> DELIVERY_VAN = BOOTSTRAP.register(roadVehicle("delivery_van", 0.72f, 1.35f, 46, 8, 18, 2, 21, 23, HornSoundDefinition.klaxon(12), WheelBase.basic(36, 13)));
    public static final ResourceKey<AutomobileFrame> PICKUP_TRUCK = BOOTSTRAP.register(roadVehicle("pickup_truck", 0.8f, 1.3f, 44, 8, 17, 2, 20, 22, HornSoundDefinition.brass(12, 16), WheelBase.basic(36, 14)));
    public static final ResourceKey<AutomobileFrame> ROADSTER = BOOTSTRAP.register(roadVehicle("roadster", 0.55f, 1.15f, 42, 6, 17, 2, 20, 21, HornSoundDefinition.trumpet(17, 19), WheelBase.basic(34, 12)));
    public static final ResourceKey<AutomobileFrame> VILLAGE_BUS = BOOTSTRAP.register(roadVehicle("village_bus", 1.1f, 1.5f, 56, 8, 22, 2, 26, 28, HornSoundDefinition.brass(6, 8), WheelBase.basic(48, 15)));
    public static final ResourceKey<AutomobileFrame> FIRE_ENGINE = BOOTSTRAP.register(roadVehicle("fire_engine", 1.05f, 1.4f, 50, 8, 19, 2, 24, 26, HornSoundDefinition.brass(5, 10), WheelBase.basic(42, 15)));
    public static final ResourceKey<AutomobileFrame> AMBULANCE = BOOTSTRAP.register(roadVehicle("ambulance", 0.95f, 1.35f, 48, 8, 19, 2, 23, 25, HornSoundDefinition.klaxon(14), WheelBase.basic(40, 14)));
    public static final ResourceKey<AutomobileFrame> MINING_DUMP_TRUCK = BOOTSTRAP.register(roadVehicle("mining_dump_truck", 1.3f, 1.5f, 50, 8, 19, 2, 24, 26, HornSoundDefinition.brass(3, 6), WheelBase.basic(42, 16)));
    public static final ResourceKey<AutomobileFrame> FARM_HARVESTER = BOOTSTRAP.register(roadVehicle("farm_harvester", 1.0f, 1.4f, 48, 8, 18, 2, 23, 27, HornSoundDefinition.brass(7, 13), WheelBase.basic(40, 16)));
    public static final ResourceKey<AutomobileFrame> DUNE_BUGGY = BOOTSTRAP.register(roadVehicle("dune_buggy", 0.65f, 1.2f, 40, 7, 16, 2, 19, 21, HornSoundDefinition.klaxon(9), WheelBase.basic(32, 14)));
    public static final ResourceKey<AutomobileFrame> MOTORBIKE = BOOTSTRAP.register(twoWheelVehicle("motorbike", 0.42f, 0.8f, 36, 7, 13, 2, 17, 18, HornSoundDefinition.klaxon(15)));
    public static final ResourceKey<AutomobileFrame> HARLEY = BOOTSTRAP.register(harley());

    public static final ResourceKey<AutomobileFrame> SHOPPING_CART = BOOTSTRAP.register(
            Automobility.rl("shopping_cart"),
            of(
                    0.25f,
                    List.of(
                            new Hitbox(new Vec3(0, 0.8, 0), 1.1f, 1.6f)
                    ),
                    HornSoundDefinition.disc(19),
                    FrameModel.legacy(
                            Automobility.rl("textures/entity/automobile/frame/shopping_cart.png"),
                            Automobility.rl("frame/shopping_cart"),
                            WheelBase.basic(17, 12.05f),
                            25,
                            11,
                            7,
                            17,
                            11,
                            11
                    )
            )
    );

    public static final ResourceKey<AutomobileFrame> C_ARR = BOOTSTRAP.register(
            Automobility.rl("c_arr"),
            of(
                    0.85f,
                    1.5f,
                    List.of(
                            new Hitbox(new Vec3(0, 1.1, 0), 1.3f, 0.9f),
                            new Hitbox(new Vec3(0, 0.4, 1.5), 1.1f, 0.8f),
                            new Hitbox(new Vec3(0, 0.4, -1.5), 1.1f, 0.8f)
                    ),
                    HornSoundDefinition.brass(6),
                    FrameModel.legacy(
                            Automobility.rl("textures/entity/automobile/frame/c_arr.png"),
                            Automobility.rl("frame/c_arr"),
                            WheelBase.basic(44.5f, 16),
                            44f,
                            6f,
                            19.5f,
                            10.5f,
                            23,
                            23
                    )
            )
    );

    public static final ResourceKey<AutomobileFrame> PINEAPPLE = BOOTSTRAP.register(
            Automobility.rl("pineapple"),
            of(
                    0.75f,
                    List.of(
                            new Hitbox(new Vec3(0, 0.6, 0), 1.1f, 1.2f)
                    ),
                    HornSoundDefinition.party(12),
                    FrameModel.legacy(
                            Automobility.rl("textures/entity/automobile/frame/pineapple.png"),
                            Automobility.rl("frame/pineapple"),
                            WheelBase.basic(10, 18),
                            20,
                            16,
                            8,
                            6,
                            9,
                            9
                    )
            )
    );

    private static DefaultRegistrar.Candidate<AutomobileFrame> standard(String color, int... hornNotes) {
        return DefaultRegistrar.cand(Automobility.rl("standard_"+color), of(
                0.6f,
                List.of(
                        new Hitbox(new Vec3(0, 0.35, 0), 1.05f, 0.7f),
                        new Hitbox(new Vec3(0, 0.3, 0.8), 1f, 0.6f),
                        new Hitbox(new Vec3(0, 0.45, -0.65), 0.8f, 0.85f)
                ),
                HornSoundDefinition.disc(hornNotes),
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/standard_"+color+".png"),
                        Automobility.rl("frame/standard"),
                        WheelBase.basic(26, 10),
                        26,
                        5,
                        13,
                        3,
                        18,
                        22
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> motorcar(String variant, float weight, HornSoundDefinition horn) {
        return DefaultRegistrar.cand(Automobility.rl(variant+"_motorcar"), of(
                weight,
                List.of(
                        new Hitbox(new Vec3(0, 0.4, 0), 1.2f, 0.8f),
                        new Hitbox(new Vec3(0, 0.45, 1), 0.9f, 0.9f),
                        new Hitbox(new Vec3(0, 0.45, -1), 0.9f, 1f)
                ),
                horn,
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/"+variant+"_motorcar.png"),
                        Automobility.rl("frame/motorcar"),
                        WheelBase.basic(32, 12),
                        28,
                        3,
                        18,
                        2,
                        23,
                        22
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> tractor(String color, int... hornNotes) {
        return DefaultRegistrar.cand(Automobility.rl(color+"_tractor"), of(
                0.9f,
                List.of(
                        new Hitbox(new Vec3(0, 0.4, 0), 1.1f, 0.8f),
                        new Hitbox(new Vec3(0, 0.6, 0.7), 0.9f, 1.2f)
                ),
                HornSoundDefinition.brass(hornNotes),
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/"+color+"_tractor.png"),
                        Automobility.rl("frame/tractor"),
                        new WheelBase(List.of(
                                new WheelBase.WheelPos(-2, -7, 1.8f, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(-2, 7, 1.8f, 180, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.RIGHT),
                                new WheelBase.WheelPos(15, -1, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(15, 1, 1, 180, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.RIGHT)
                        )),
                        24,
                        9,
                        9,
                        8,
                        12,
                        19
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> rickshaw(String prefix, float weight, HornSoundDefinition horn) {
        return DefaultRegistrar.cand(Automobility.rl(prefix+"_rickshaw"), of(
                weight,
                List.of(
                        new Hitbox(new Vec3(0, 0.4, -0.2), 0.6f, 0.3f),
                        new Hitbox(new Vec3(0, 0.4, 0.6), 0.7f, 0.8f),
                        new Hitbox(new Vec3(0, 0.8, -0.8), 0.9f, 1.6f)
                ),
                horn,
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/"+prefix+"_rickshaw.png"),
                        Automobility.rl("frame/rickshaw"),
                        new WheelBase(List.of(
                                new WheelBase.WheelPos(-11, -7.5f, 1, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(-11, 7.5f, 1, 180, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.RIGHT),
                                new WheelBase.WheelPos(11, -0.1f, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(11, 0.1f, 1, 180, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.RIGHT)
                        )),
                        26,
                        2.5f,
                        13,
                        3,
                        17.5f,
                        14.5f
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> jeep(String color) {
        return DefaultRegistrar.cand(Automobility.rl("jeep_" + color), of(
                0.9f,
                1.35f,
                List.of(
                        new Hitbox(new Vec3(0, 0.45, 0), 1.25f, 0.9f),
                        new Hitbox(new Vec3(0, 0.45, 1.2), 1.05f, 0.9f),
                        new Hitbox(new Vec3(0, 0.45, -1.2), 1.05f, 0.9f)
                ),
                HornSoundDefinition.brass(16, 19),
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/jeep_" + color + ".png"),
                        Automobility.rl("frame/jeep"),
                        new WheelBase(List.of(
                                new WheelBase.WheelPos(-12, -6.8f, 1, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(-12, 6.8f, 1, 180, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.RIGHT),
                                new WheelBase.WheelPos(12, -6.8f, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(12, 6.8f, 1, 180, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.RIGHT)
                        )),
                        45,
                        7,
                        9,
                        2,
                        18,
                        19
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> beetle(String color) {
        return beetle(color, 0.65f);
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> beetle(String color, float weight) {
        return DefaultRegistrar.cand(Automobility.rl("beetle_" + color), of(
                weight,
                1.35f,
                List.of(
                        new Hitbox(new Vec3(0, 0.45, 0), 1.2f, 0.85f),
                        new Hitbox(new Vec3(0, 0.45, 1.2), 1.05f, 0.85f),
                        new Hitbox(new Vec3(0, 0.45, -1.2), 1.05f, 0.85f)
                ),
                HornSoundDefinition.klaxon(11),
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/beetle_" + color + ".png"),
                        Automobility.rl("frame/beetle"),
                        new WheelBase(List.of(
                                new WheelBase.WheelPos(-15, -9f, 1, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(-15, 9f, 1, 180, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.RIGHT),
                                new WheelBase.WheelPos(14.5f, -9f, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.LEFT),
                                new WheelBase.WheelPos(14.5f, 9f, 1, 180, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.RIGHT)
                        )),
                        45,
                        2,
                        14,
                        2,
                        23,
                        24
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> roadVehicle(String id, float weight, float width,
                                                                            float lengthPx, float seatHeight,
                                                                            float engineBack, float engineUp,
                                                                            float rearAttachmentPos, float frontAttachmentPos,
                                                                            HornSoundDefinition horn, WheelBase wheels) {
        return DefaultRegistrar.cand(Automobility.rl(id), of(
                weight,
                width,
                List.of(
                        new Hitbox(new Vec3(0, 0.4, 0), Math.min(1.3f, width), 0.85f),
                        new Hitbox(new Vec3(0, 0.45, lengthPx / 32f), Math.min(1.15f, width), 0.8f),
                        new Hitbox(new Vec3(0, 0.45, -lengthPx / 32f), Math.min(1.15f, width), 0.8f)
                ),
                horn,
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/" + id + ".png"),
                        Automobility.rl("frame/" + id),
                        wheels,
                        lengthPx,
                        seatHeight,
                        engineBack,
                        engineUp,
                        rearAttachmentPos,
                        frontAttachmentPos
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> twoWheelVehicle(String id, float weight, float width,
                                                                                 float lengthPx, float seatHeight,
                                                                                 float engineBack, float engineUp,
                                                                                 float rearAttachmentPos, float frontAttachmentPos,
                                                                                 HornSoundDefinition horn) {
        return DefaultRegistrar.cand(Automobility.rl(id), of(
                weight,
                width,
                List.of(
                        new Hitbox(new Vec3(0, 0.45, 0), 0.8f, 0.75f),
                        new Hitbox(new Vec3(0, 0.7, -0.35), 0.65f, 0.65f)
                ),
                horn,
                FrameModel.legacy(
                        Automobility.rl("textures/entity/automobile/frame/" + id + ".png"),
                        Automobility.rl("frame/" + id),
                        new WheelBase(List.of(
                                new WheelBase.WheelPos(-lengthPx / 2, 0, 1, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.CENTER),
                                new WheelBase.WheelPos(lengthPx / 2, 0, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.CENTER)
                        )),
                        lengthPx,
                        seatHeight,
                        engineBack,
                        engineUp,
                        rearAttachmentPos,
                        frontAttachmentPos
                )
        ));
    }

    private static DefaultRegistrar.Candidate<AutomobileFrame> harley() {
        return DefaultRegistrar.cand(Automobility.rl("harley"), of(
                0.48f,
                0.8f,
                List.of(
                        new Hitbox(new Vec3(0, 0.45, 0), 0.8f, 0.75f),
                        new Hitbox(new Vec3(0, 0.7, -0.35), 0.65f, 0.65f)
                ),
                HornSoundDefinition.klaxon(15),
                new FrameModel(
                        Automobility.rl("textures/entity/automobile/frame/harley.png"),
                        Automobility.rl("frame/harley"),
                        new WheelBase(List.of(
                                new WheelBase.WheelPos(14.26f, 0, 1, 0, WheelBase.WheelEnd.FRONT, WheelBase.WheelSide.CENTER),
                                new WheelBase.WheelPos(-14.26f, 0, 1, 0, WheelBase.WheelEnd.BACK, WheelBase.WheelSide.CENTER)
                        )),
                        40,
                        new Vec3(0, 14, -3.5),
                        List.of(),
                        new Vec3(0, 2, -8),
                        15,
                        20
                )
        ));
    }

    public static final DisplayStat<AutomobileFrame> STAT_WEIGHT = new DisplayStat<>("weight", AutomobileFrame::weight);

    @Override
    public boolean isEmpty() {
        return empty();
    }

    @Override
    public ResourceLocation containerId() {
        return ID;
    }

    @Override
    public void forEachStat(Consumer<DisplayStat<AutomobileFrame>> action) {
        action.accept(STAT_WEIGHT);
    }

    public static String getTranslationKey(ResourceLocation id) {
        return "frame."+id.getNamespace()+"."+id.getPath();
    }

    @Override
    public ResourceLocation getId() {
        return LEGACY_IDS.getOrDefault(this, Automobility.rl("invalid"));
    }

    public record Hitbox(
            Vec3 origin,
            float width,
            float height
    ) {
        public static final Codec<Hitbox> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Vec3.CODEC.fieldOf("origin").forGetter(Hitbox::origin),
                Codec.FLOAT.fieldOf("width").forGetter(Hitbox::width),
                Codec.FLOAT.fieldOf("height").forGetter(Hitbox::height)
        ).apply(inst, Hitbox::new));

        public static final StreamCodec<ByteBuf, Hitbox> STREAM_CODEC = StreamCodec.composite(
                AUtils.STREAM_CODEC_VEC3, Hitbox::origin,
                ByteBufCodecs.FLOAT, Hitbox::width,
                ByteBufCodecs.FLOAT, Hitbox::height,
                Hitbox::new
        );

        public static final Hitbox DEFAULT = new Hitbox(
                new Vec3(0, 0.35, 0), 1.1f, 0.7f
        );
    }

    public record FrameModel(
            ResourceLocation texture,
            ResourceLocation modelId,
            WheelBase wheelBase,
            float lengthPx,
            Vec3 driverSeatPos,
            List<Vec3> passengerSeats,
            Vec3 enginePos,
            float rearAttachmentPos,
            float frontAttachmentPos
    ) {
        /** Compatibility constructor for 0.4.x JSON/model definitions. */
        public FrameModel(ResourceLocation texture, ResourceLocation modelId, WheelBase wheelBase,
                          float lengthPx, float seatHeight, float enginePosBack, float enginePosUp,
                          float rearAttachmentPos, float frontAttachmentPos) {
            this(texture, modelId, wheelBase, lengthPx, new Vec3(0, seatHeight, 0), List.of(),
                    new Vec3(0, enginePosUp, -enginePosBack), rearAttachmentPos, frontAttachmentPos);
        }
        public static final Codec<FrameModel> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(FrameModel::texture),
                ResourceLocation.CODEC.fieldOf("model").forGetter(FrameModel::modelId),
                WheelBase.CODEC.fieldOf("wheels").forGetter(FrameModel::wheelBase),
                Codec.FLOAT.fieldOf("length_pixels").forGetter(FrameModel::lengthPx),
                Vec3.CODEC.fieldOf("driver_seat").forGetter(FrameModel::driverSeatPos),
                Codec.list(Vec3.CODEC).optionalFieldOf("passenger_seats", List.of()).forGetter(FrameModel::passengerSeats),
                Vec3.CODEC.fieldOf("engine_position").forGetter(FrameModel::enginePos),
                Codec.FLOAT.fieldOf("rear_attachment_offset").forGetter(FrameModel::rearAttachmentPos),
                Codec.FLOAT.fieldOf("front_attachment_offset").forGetter(FrameModel::frontAttachmentPos)
        ).apply(inst, FrameModel::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FrameModel> STREAM_CODEC = StreamCodec.of(
                (buf, m) -> {
                    buf.writeResourceLocation(m.texture());
                    buf.writeResourceLocation(m.modelId());
                    WheelBase.STREAM_CODEC.encode(buf, m.wheelBase());
                    buf.writeFloat(m.lengthPx());
                    AUtils.STREAM_CODEC_VEC3.encode(buf, m.driverSeatPos());
                    ByteBufCodecs.<ByteBuf, Vec3>list().apply(AUtils.STREAM_CODEC_VEC3).encode(buf, m.passengerSeats());
                    AUtils.STREAM_CODEC_VEC3.encode(buf, m.enginePos());
                    buf.writeFloat(m.rearAttachmentPos());
                    buf.writeFloat(m.frontAttachmentPos());
                },
                buf -> new FrameModel(
                        buf.readResourceLocation(),
                        buf.readResourceLocation(),
                        WheelBase.STREAM_CODEC.decode(buf),
                        buf.readFloat(),
                        AUtils.STREAM_CODEC_VEC3.decode(buf),
                        ByteBufCodecs.<ByteBuf, Vec3>list().apply(AUtils.STREAM_CODEC_VEC3).decode(buf),
                        AUtils.STREAM_CODEC_VEC3.decode(buf),
                        buf.readFloat(),
                        buf.readFloat()
                )
        );

        public static FrameModel legacy(ResourceLocation texture,
                                        ResourceLocation modelId,
                                        WheelBase wheelBase,
                                        float lengthPx,
                                        float seatHeight,
                                        float enginePosBack,
                                        float enginePosUp,
                                        float rearAttachmentPos,
                                        float frontAttachmentPos) {
            return new FrameModel(
                    texture, modelId, wheelBase, lengthPx,
                    new Vec3(0, seatHeight, 0),
                    List.of(),
                    new Vec3(0, enginePosUp, -enginePosBack),
                    rearAttachmentPos, frontAttachmentPos
            );
        }
    }
}
