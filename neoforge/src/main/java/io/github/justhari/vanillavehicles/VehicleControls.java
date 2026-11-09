package io.github.justhari.vanillavehicles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VehicleControls(float forward, float strafe, float yaw, float pitch, boolean jump, boolean attack) implements CustomPacketPayload {
    public static final Type<VehicleControls> TYPE = new Type<>(VanillaVehicles.rl("controls"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleControls> CODEC = StreamCodec.of((buf,p) -> {
        buf.writeFloat(p.forward); buf.writeFloat(p.strafe); buf.writeFloat(p.yaw); buf.writeFloat(p.pitch); buf.writeBoolean(p.jump); buf.writeBoolean(p.attack);
    }, buf -> new VehicleControls(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE,CODEC,VehicleControls::handle);
    }
    private static void handle(VehicleControls input, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getControllingPassenger() == player) vehicle.acceptInput(input);
        });
    }
}
