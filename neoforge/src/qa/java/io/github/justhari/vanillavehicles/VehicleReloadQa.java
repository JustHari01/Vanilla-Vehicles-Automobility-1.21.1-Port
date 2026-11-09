package io.github.justhari.vanillavehicles;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Verifies persisted entities in a second server process; excluded from release builds. */
@EventBusSubscriber(modid=VanillaVehicles.MOD_ID)
public class VehicleReloadQa {
    private static MinecraftServer server;
    private static JsonArray rows;
    private static final Set<UUID> checked=new HashSet<>();
    private static int tick;
    private static boolean failed;
    private static void check(boolean ok,String message){
        if(!ok)failed=true;
        if(ok)LogUtils.getLogger().info("[VV RELOAD QA] PASS {}",message);
        else LogUtils.getLogger().error("[VV RELOAD QA] FAIL {}",message);
    }
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!Boolean.getBoolean("vv.reloadQa"))return;
        server=event.getServer();
        try{
            rows=JsonParser.parseString(Files.readString(Path.of("vv-persistence-proof.json"))).getAsJsonArray();
            check(rows.size()==8,"saved proof contains all eight vehicle types");
            var level=server.overworld();
            for(var raw:rows){var row=raw.getAsJsonObject();int x=(int)Math.floor(row.get("x").getAsDouble())>>4,z=(int)Math.floor(row.get("z").getAsDouble())>>4;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){level.setChunkForced(x+dx,z+dz,true);level.getChunk(x+dx,z+dz);}
            }
        }catch(Throwable error){failed=true;LogUtils.getLogger().error("[VV RELOAD QA] Could not load saved proof",error);server.halt(false);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(server==null||event.getServer()!=server||server.isStopped())return;
        tick++;
        try{
            for(var raw:rows){var row=raw.getAsJsonObject();var uuid=UUID.fromString(row.get("uuid").getAsString());if(checked.contains(uuid))continue;
                var entity=server.overworld().getEntity(uuid);if(entity==null)continue;
                String id=row.get("id").getAsString();check(entity instanceof VehicleEntity,id+" original UUID restored from saved world");
                if(entity instanceof VehicleEntity v){
                    check(v.kind.id.equals(id),id+" vehicle type restored");
                    check(Math.abs(v.fuel()-row.get("fuel").getAsFloat())<.1f,id+" saved fuel restored");
                    check(v.color()==row.get("color").getAsInt()&&v.variant("sticker").equals(row.get("sticker").getAsString()),id+" dye and sticker restored");
                    check(v.inventory.getItem(0).is(net.minecraft.world.item.Items.DIAMOND)&&v.inventory.getItem(0).getCount()==row.get("diamonds").getAsInt(),id+" stored items restored");
                }
                checked.add(uuid);
            }
            if(tick==300){check(checked.size()==8,"all eight saved vehicles loaded in a new server process");LogUtils.getLogger().info("[VV RELOAD QA] RESULT {}",failed?"FAILED":"PASSED");server.halt(false);}
        }catch(Throwable error){failed=true;LogUtils.getLogger().error("[VV RELOAD QA] Runtime exception",error);server.halt(false);}
    }
}
