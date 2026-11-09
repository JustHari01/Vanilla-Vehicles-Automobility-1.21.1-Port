package io.github.justhari.vanillavehicles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Players may change only their own original guidebook preferences. */
public record VehicleSettings(boolean fuelHud,boolean windSound,boolean windParticles) implements CustomPacketPayload {
    public static final Type<VehicleSettings> TYPE=new Type<>(VanillaVehicles.rl("settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf,VehicleSettings> CODEC=StreamCodec.of((b,s)->{b.writeBoolean(s.fuelHud);b.writeBoolean(s.windSound);b.writeBoolean(s.windParticles);},b->new VehicleSettings(b.readBoolean(),b.readBoolean(),b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(TYPE,CODEC,(s,ctx)->ctx.enqueueWork(()->{
        var tag=ctx.player().getPersistentData();tag.putBoolean("vv_hide_fuel_hud",!s.fuelHud);tag.putBoolean("vv_wind_sound",s.windSound);tag.putBoolean("vv_wind_particles",s.windParticles);
    }));}
}
