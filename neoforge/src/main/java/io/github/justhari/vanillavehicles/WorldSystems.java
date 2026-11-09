package io.github.justhari.vanillavehicles;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid=VanillaVehicles.MOD_ID)
public class WorldSystems {
    public static int windIndex(Level level){return level instanceof ServerLevel server?WindData.get(server).direction:(int)Math.floorMod(level.getGameTime()/72000,4);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        var p=event.getEntity();var tag=p.getPersistentData();
        if(!tag.getBoolean("vv_welcomed")){p.addItem(new ItemStack(VanillaVehicles.item("guide_book")));tag.putBoolean("vv_welcomed",true);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var level=event.getServer().overworld();long time=level.getGameTime();
        if(time%20==0){var data=WindData.get(level);if(time-data.lastChange>=72000){data.direction=(data.direction+3)%4;data.lastChange=time;data.setDirty();}}
        if(time%80==0)for(var player:event.getServer().getPlayerList().getPlayers()){
            if(player.getY()<64||!player.serverLevel().canSeeSky(player.blockPosition()))continue;
            var preferences=player.getPersistentData();
            if(preferences.getBoolean("vv_wind_particles"))player.serverLevel().sendParticles(player,net.minecraft.core.particles.ParticleTypes.CLOUD,false,player.getX(),player.getY()+1,player.getZ(),2,3,.1,3,.02);
            if(time%400==0&&preferences.getBoolean("vv_wind_sound")){var sound=VanillaVehicles.sound("bw_vv.ui.ambient.wind");if(sound!=null)player.playNotifySound(sound,net.minecraft.sounds.SoundSource.AMBIENT,.3f,1);}
        }
        if(time%1200!=0||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        for(var player:level.players()){
            for(int attempt=0;attempt<8;attempt++){
                int x=player.getBlockX()+level.random.nextIntBetweenInclusive(-64,64),z=player.getBlockZ()+level.random.nextIntBetweenInclusive(-64,64);
                if(!level.hasChunk(x>>4,z>>4))continue;
                var surface=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
                if(!level.hasChunkAt(surface))continue;
                var below=level.getBlockState(surface.below());
                if(level.isVillage(surface)&&(below.is(Blocks.COBBLESTONE)||below.is(Blocks.DIRT_PATH)||below.is(BlockTags.PLANKS))&&level.getEntitiesOfClass(MechanicEntity.class,new net.minecraft.world.phys.AABB(surface).inflate(96)).isEmpty()){
                    var mechanic=VanillaVehicles.MECHANIC.get().create(level);mechanic.setPos(x+.5,surface.getY(),z+.5);if(level.noCollision(mechanic))level.addFreshEntity(mechanic);break;
                }
                if(level.getBiome(surface).is(BiomeTags.IS_OCEAN)){
                    for(int y=surface.getY()-2;y>Math.max(player.getBlockY()-32,level.getMinBuildHeight());y--){
                        var floor=new BlockPos(x,y,z);var state=level.getBlockState(floor);
                        if((state.is(Blocks.PRISMARINE)||state.is(Blocks.PRISMARINE_BRICKS)||state.is(Blocks.DARK_PRISMARINE)||state.is(Blocks.SEA_LANTERN))&&level.getFluidState(floor.above()).is(FluidTags.WATER)&&level.getMaxLocalRawBrightness(floor.above())<=10&&level.getEntitiesOfClass(VehicleEntity.class,new net.minecraft.world.phys.AABB(floor).inflate(64),v->v.kind==VehicleKind.PRISMER).size()<2){
                            var prismer=VanillaVehicles.VEHICLES.get(VehicleKind.PRISMER).get().create(level);prismer.setPos(x+.5,y+1,z+.5);if(level.noCollision(prismer))level.addFreshEntity(prismer);break;
                        }
                    }
                }
            }
        }
    }
    public static class WindData extends SavedData {
        int direction;long lastChange;
        private static WindData get(ServerLevel level){var overworld=level.getServer().overworld();return overworld.getDataStorage().computeIfAbsent(new Factory<>(()->{var data=new WindData();data.lastChange=overworld.getGameTime();data.setDirty();return data;},WindData::load,null),"vanilla_vehicles_wind");}
        private static WindData load(CompoundTag tag,HolderLookup.Provider lookup){var data=new WindData();data.direction=Math.floorMod(tag.getInt("Direction"),4);data.lastChange=tag.getLong("LastChange");return data;}
        @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.putInt("Direction",direction);tag.putLong("LastChange",lastChange);return tag;}
    }
}
