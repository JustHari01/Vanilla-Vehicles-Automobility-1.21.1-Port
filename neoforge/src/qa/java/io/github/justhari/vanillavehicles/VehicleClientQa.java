package io.github.justhari.vanillavehicles;

import java.nio.file.*;
import java.util.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=VanillaVehicles.MOD_ID,value=Dist.CLIENT)
public class VehicleClientQa {
    private static boolean requested,prepared,failed;
    private static UUID compressorId,mechanicId;
    private static int tick;
    private static final List<UUID> entities=new ArrayList<>();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("vv.clientQa"))return;
        var mc=Minecraft.getInstance();
        if(!requested&&mc.getOverlay()==null&&mc.screen!=null){
            requested=true;
            mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;
            var settings=new LevelSettings("Vanilla Vehicles Local QA",net.minecraft.world.level.GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("vv-client-qa-"+System.currentTimeMillis(),settings,new WorldOptions(42,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
            return;
        }
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        if(!prepared){
            prepared=true;
            var server=mc.getSingleplayerServer();
            server.execute(()->{
                var level=server.overworld();level.setDayTime(6000);
                for(int x=-10;x<95;x++)for(int z=-12;z<100;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),2);
                int i=0;
                for(var kind:VehicleKind.values()){
                    var v=VanillaVehicles.VEHICLES.get(kind).get().create(level);v.setPos(i++*10+.5,80,.5);v.setFuel(10);v.setYRot(180);
                    if(kind==VehicleKind.BALLOON)v.setVariant("envelope","blank");
                    if(kind==VehicleKind.STEAMBOAT)v.setFlag("bell",true);
                    if(kind==VehicleKind.MINECAR||kind==VehicleKind.MOBTRUCK){v.setColor(14);v.setVariant("sticker","creeper");}
                    if(kind==VehicleKind.PLANE)v.setColor(14);
                    if(kind==VehicleKind.BALLOON)v.setColor(6);
                    if(kind==VehicleKind.STEAMBOAT)v.setColor(3);
                    if(kind==VehicleKind.MECH)v.setColor(11);
                    if(kind==VehicleKind.PRISMER){for(int x=57;x<=64;x++)for(int z=-4;z<5;z++)for(int y=80;y<82;y++)level.setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),2);}
                    v.setFlag("chest",true);level.addFreshEntity(v);entities.add(v.getUUID());
                }
                level.setBlock(new BlockPos(6,80,0),VanillaVehicles.WIND_VANE.get().defaultBlockState(),3);
                level.setBlock(new BlockPos(5,80,0),VanillaVehicles.FLAGS.get("flag.pole").get().defaultBlockState(),3);
                level.setBlock(new BlockPos(5,81,0),VanillaVehicles.FLAGS.get("flag.red").get().defaultBlockState(),3);
                var compressor=VanillaVehicles.COMPRESSOR.get().create(level);compressor.setPos(7,80,0);compressor.inventory.setItem(0,new net.minecraft.world.item.ItemStack(VanillaVehicles.item("compressed_redstone"),8));level.addFreshEntity(compressor);compressorId=compressor.getUUID();
                var mechanic=VanillaVehicles.MECHANIC.get().create(level);mechanic.setPos(8,80,0);mechanic.setNoAi(true);mechanic.setVillagerXp(60);level.addFreshEntity(mechanic);mechanic.getOffers();mechanic.updateTrades();mechanicId=mechanic.getUUID();
                var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(level,4,83,8,150,20);p.getAbilities().flying=true;p.onUpdateAbilities();
            });
            return;
        }
        tick++;
        int index=(tick-1)/60;
        if(index<8){
            if(tick%60==1){
                int x=index*10;mc.getSingleplayerServer().execute(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.teleportTo(mc.getSingleplayerServer().overworld(),x+5,83,8,145,17);p.getAbilities().flying=true;p.onUpdateAbilities();
                });
            }
            if(tick%60==50){
                String name="vv-"+VehicleKind.values()[index].id+".png";
                Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->LogUtils.getLogger().info("[VV CLIENT QA] Screenshot {}",name));
                LogUtils.getLogger().info("[VV CLIENT QA] Rendered {} with client entity count {}",name,mc.level.getEntityCount());
            }
        }
        if(tick==500){mc.options.hideGui=false;mc.setScreen(new VehicleClient.GuideScreen());}
        if(tick==525)Screenshot.grab(mc.gameDirectory,"vv-guide.png",mc.getMainRenderTarget(),message->{});
        if(tick==528&&mc.screen instanceof VehicleClient.GuideScreen guide)guide.showPage("bw_vv.guide_book.vehicle_page.plane.");
        if(tick==545)Screenshot.grab(mc.gameDirectory,"vv-guide-plane.png",mc.getMainRenderTarget(),message->{});
        if(tick==550){
            mc.setScreen(null);mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var v=(VehicleEntity)mc.getSingleplayerServer().overworld().getEntity(entities.get(1));p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(mc.getSingleplayerServer().overworld(),v.getX(),v.getY()+1,v.getZ(),0,0);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(VanillaVehicles.item("tesla_helmet")));p.startRiding(v);});
        }
        if(tick>=570&&tick<700){mc.options.keyUp.setDown(true);mc.options.keyJump.setDown(tick>=630&&tick<635);}
        if(tick==700){
            mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);
            mc.getSingleplayerServer().execute(()->{var v=(VehicleEntity)mc.getSingleplayerServer().overworld().getEntity(entities.get(1));if(v.getZ()<=5)failed=true;LogUtils.getLogger().info("[VV CLIENT QA] {} real client controls advanced vehicle to z={}, fuel={}",v.getZ()>5?"PASS":"FAIL",v.getZ(),v.fuel());});
            Screenshot.grab(mc.gameDirectory,"vv-driving.png",mc.getMainRenderTarget(),message->{});
        }
        if(tick==730){
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var v=(VehicleEntity)mc.getSingleplayerServer().overworld().getEntity(entities.get(1));p.stopRiding();p.teleportTo(mc.getSingleplayerServer().overworld(),v.getX()+2,v.getY(),v.getZ(),0,0);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);v.interact(p,InteractionHand.MAIN_HAND);});
        }
        if(tick==760){Screenshot.grab(mc.gameDirectory,"vv-storage.png",mc.getMainRenderTarget(),message->{});check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,"vehicle storage GUI");}
        if(tick==790){mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.closeContainer();p.setShiftKeyDown(false);p.teleportTo(mc.getSingleplayerServer().overworld(),7,80,2,180,0);p.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);mc.getSingleplayerServer().overworld().getEntity(compressorId).interact(p,InteractionHand.MAIN_HAND);});}
        if(tick==820){Screenshot.grab(mc.gameDirectory,"vv-compressor-storage.png",mc.getMainRenderTarget(),message->{});check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,"compressor storage GUI");}
        if(tick==850){mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.closeContainer();p.setShiftKeyDown(false);p.teleportTo(mc.getSingleplayerServer().overworld(),8,80,2,180,0);p.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);mc.getSingleplayerServer().overworld().getEntity(mechanicId).interact(p,InteractionHand.MAIN_HAND);});}
        if(tick==880){Screenshot.grab(mc.gameDirectory,"vv-mechanic-trades.png",mc.getMainRenderTarget(),message->{});check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen,"Mechanic trading GUI");}
        if(tick==910){LogUtils.getLogger().info("[VV CLIENT QA] RESULT {} (render, network controls, guide, storage, compressor, trading)",failed?"FAILED":"PASSED");mc.stop();}
    }
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("[VV CLIENT QA] {} {}",ok?"PASS":"FAIL",message);}
}
