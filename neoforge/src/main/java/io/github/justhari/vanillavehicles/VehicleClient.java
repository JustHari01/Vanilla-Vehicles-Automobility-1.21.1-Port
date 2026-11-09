package io.github.justhari.vanillavehicles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=VanillaVehicles.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class VehicleClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(VanillaVehicles.VANE_ENTITY.get(),ctx->new software.bernie.geckolib.renderer.GeoBlockRenderer<>(new software.bernie.geckolib.model.GeoModel<WindVane.VaneEntity>(){
            @Override public net.minecraft.resources.ResourceLocation getModelResource(WindVane.VaneEntity v){return VanillaVehicles.rl("geo/wind_vane.geo.json");}
            @Override public net.minecraft.resources.ResourceLocation getTextureResource(WindVane.VaneEntity v){return VanillaVehicles.rl("textures/bw/vv/blocks/wind_vane.png");}
            @Override public net.minecraft.resources.ResourceLocation getAnimationResource(WindVane.VaneEntity v){return VanillaVehicles.rl("animations/vehicle.animation.json");}
            @Override public void setCustomAnimations(WindVane.VaneEntity v,long id,software.bernie.geckolib.animation.AnimationState<WindVane.VaneEntity> state){super.setCustomAnimations(v,id,state);getBone("main").ifPresent(b->b.setRotY((2+v.getBlockState().getValue(WindVane.WIND))*net.minecraft.util.Mth.HALF_PI));}
        }));
        VanillaVehicles.VEHICLES.values().forEach(type->event.registerEntityRenderer(type.get(),VehicleRenderer::new));
        event.registerEntityRenderer(VanillaVehicles.PROJECTILE.get(),ctx->new software.bernie.geckolib.renderer.GeoEntityRenderer<>(ctx,new software.bernie.geckolib.model.GeoModel<VehicleProjectile>(){
            @Override public net.minecraft.resources.ResourceLocation getModelResource(VehicleProjectile p){return VanillaVehicles.rl("geo/mech_projectile.geo.json");}
            @Override public net.minecraft.resources.ResourceLocation getTextureResource(VehicleProjectile p){return VanillaVehicles.rl("textures/bw/vv/helpers/mech_projectile.png");}
            @Override public net.minecraft.resources.ResourceLocation getAnimationResource(VehicleProjectile p){return VanillaVehicles.rl("animations/vehicle.animation.json");}
            @Override public net.minecraft.client.renderer.RenderType getRenderType(VehicleProjectile p,net.minecraft.resources.ResourceLocation texture){return net.minecraft.client.renderer.RenderType.entityTranslucentEmissive(texture);}
        }));
        event.registerBlockEntityRenderer(VanillaVehicles.FLAG_ENTITY.get(),ctx->new software.bernie.geckolib.renderer.GeoBlockRenderer<>(new software.bernie.geckolib.model.GeoModel<FlagBlock.FlagEntity>(){
            @Override public net.minecraft.resources.ResourceLocation getModelResource(FlagBlock.FlagEntity flag){return VanillaVehicles.rl("geo/"+(((FlagBlock)flag.getBlockState().getBlock()).color.equals("pole")?"flag_pole":"flag")+".geo.json");}
            @Override public net.minecraft.resources.ResourceLocation getTextureResource(FlagBlock.FlagEntity flag){String color=((FlagBlock)flag.getBlockState().getBlock()).color;return VanillaVehicles.rl("textures/bw/vv/blocks/"+(color.equals("pole")?"flag.pole":"flags/"+color)+".png");}
            @Override public net.minecraft.resources.ResourceLocation getAnimationResource(FlagBlock.FlagEntity flag){return VanillaVehicles.rl("animations/vehicle.animation.json");}
            @Override public void setCustomAnimations(FlagBlock.FlagEntity flag,long id,software.bernie.geckolib.animation.AnimationState<FlagBlock.FlagEntity> state){super.setCustomAnimations(flag,id,state);getBone("block").ifPresent(b->b.setRotY((2+flag.getBlockState().getValue(WindVane.WIND))*net.minecraft.util.Mth.HALF_PI));}
        }));
        event.registerEntityRenderer(VanillaVehicles.COMPRESSOR.get(),UtilityRenderer::new);
        event.registerEntityRenderer(VanillaVehicles.SMOKE.get(),UtilityRenderer::new);
        event.registerEntityRenderer(VanillaVehicles.MECHANIC.get(),MechanicRenderer::new);
    }
    @EventBusSubscriber(modid=VanillaVehicles.MOD_ID,value=Dist.CLIENT)
    public static class Controls {
        private static net.minecraft.client.multiplayer.ClientPacketListener lastConnection;
        @SubscribeEvent public static void tick(ClientTickEvent.Post event){
            var mc=Minecraft.getInstance();
            if(mc.getConnection()!=lastConnection&&(mc.getConnection()==null||mc.player!=null)){lastConnection=mc.getConnection();if(lastConnection!=null)GuideScreen.sendPreferences();}
            if(mc.player==null||!(mc.player.getVehicle() instanceof VehicleEntity v)||v.getControllingPassenger()!=mc.player)return;
            boolean active=mc.screen==null;
            float forward=active?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
            float strafe=active?(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0):0;
            PacketDistributor.sendToServer(new VehicleControls(forward,strafe,mc.player.getYRot(),mc.player.getXRot(),active&&mc.options.keyJump.isDown(),active&&mc.options.keyAttack.isDown()));
        }
    }
    public static void openGuide(){Minecraft.getInstance().setScreen(new GuideScreen());}
    public static class GuideScreen extends Screen {
        private final java.util.List<String> pages=new java.util.ArrayList<>();
        private final com.google.gson.JsonObject content;
        private String page="";
        private int offset,scroll;
        private static boolean hud=true,windSound=false,windParticles=false;
        private int left,pane,widthText,rows;
        public GuideScreen(){
            super(Component.literal("Vanilla Vehicles"));
            try(var input=VehicleClient.class.getResourceAsStream("/assets/bw_vv/guide-pages.json")){
                content=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            pages.addAll(content.keySet());pages.add("settings");readPreferences();
        }
        private static java.nio.file.Path preferences(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/vanilla-vehicles-client.properties");}
        private static void readPreferences(){
            if(!java.nio.file.Files.exists(preferences()))return;
            var props=new java.util.Properties();try(var in=java.nio.file.Files.newInputStream(preferences())){
                props.load(in);hud=Boolean.parseBoolean(props.getProperty("fuelHud","true"));windSound=Boolean.parseBoolean(props.getProperty("windSound","false"));windParticles=Boolean.parseBoolean(props.getProperty("windParticles","false"));
            }catch(java.io.IOException e){com.mojang.logging.LogUtils.getLogger().warn("Could not read Vanilla Vehicles preferences",e);}
        }
        public static void sendPreferences(){readPreferences();PacketDistributor.sendToServer(new VehicleSettings(hud,windSound,windParticles));}
        private static void savePreferences(){
            var props=new java.util.Properties();props.setProperty("fuelHud",Boolean.toString(hud));props.setProperty("windSound",Boolean.toString(windSound));props.setProperty("windParticles",Boolean.toString(windParticles));
            try{java.nio.file.Files.createDirectories(preferences().getParent());try(var out=java.nio.file.Files.newOutputStream(preferences())){props.store(out,"Vanilla Vehicles guidebook settings");}}
            catch(java.io.IOException e){com.mojang.logging.LogUtils.getLogger().warn("Could not save Vanilla Vehicles preferences",e);}
            PacketDistributor.sendToServer(new VehicleSettings(hud,windSound,windParticles));
        }
        public void showPage(String name){page=name;scroll=0;rebuildWidgets();}
        @Override protected void init(){
            left=Math.max(10,width/2-190);pane=left+118;widthText=Math.max(100,width-pane-14);rows=Math.max(3,(height-98)/20);
            for(int i=0;i<rows&&offset+i<pages.size();i++){
                String key=pages.get(offset+i);Component label=key.equals("settings")?Component.literal("Settings"):Component.literal(Component.translatable(key+"title").getString().replace("Vehicles - ","").replace("Utilities - ","").replace("Systems - ","").replace("Mobs - ",""));
                addRenderableWidget(Button.builder(label,b->showPage(key)).bounds(left,34+i*20,108,18).build());
            }
            int y=34+rows*20;
            addRenderableWidget(Button.builder(Component.literal("<"),b->{offset=Math.max(0,offset-rows);rebuildWidgets();}).bounds(left,y,52,18).build());
            addRenderableWidget(Button.builder(Component.literal(">"),b->{offset=Math.min(Math.max(0,pages.size()-rows),offset+rows);rebuildWidgets();}).bounds(left+56,y,52,18).build());
            addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width/2-40,height-25,80,20).build());
            if(page.equals("settings")){
                addRenderableWidget(Button.builder(Component.literal("Fuel HUD: "+hud),b->{hud=!hud;savePreferences();rebuildWidgets();}).bounds(pane,58,widthText,20).build());
                addRenderableWidget(Button.builder(Component.literal("Wind sound: "+windSound),b->{windSound=!windSound;savePreferences();rebuildWidgets();}).bounds(pane,84,widthText,20).build());
                addRenderableWidget(Button.builder(Component.literal("Wind particles: "+windParticles),b->{windParticles=!windParticles;savePreferences();rebuildWidgets();}).bounds(pane,110,widthText,20).build());
            }
        }
        @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
            if(x<pane){offset=net.minecraft.util.Mth.clamp(offset+(vertical<0?1:-1),0,Math.max(0,pages.size()-rows));rebuildWidgets();}
            else scroll=Math.max(0,scroll-(int)(vertical*24));return true;
        }
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
            renderBackground(g,mouseX,mouseY,partial);super.render(g,mouseX,mouseY,partial);g.drawCenteredString(font,title,width/2,13,0xffffff);
            if(page.equals("settings")){g.drawString(font,"Guidebook preferences",pane,36,0xffffff);return;}
            g.enableScissor(pane,34,width-10,height-31);int y=36-scroll;
            if(page.isEmpty()){
                g.drawWordWrap(font,Component.literal("Local test build. Right-click to enter. WASD to drive, mouse to steer. Space: boost with Tesla Helmet / balloon lift / mech power jump. Left-click: plane roll / mech weapon / ship bell. Shift-right-click: storage. Use arrows or scroll for all original guide pages."),pane,y,widthText,0xffffff);
            }else if(content.has(page)){
                for(var entry:content.getAsJsonArray(page)){
                    var text=Component.translatable(entry.getAsString());g.drawWordWrap(font,text,pane,y,widthText,0xffffff);y+=font.split(text,widthText).size()*10+8;
                }
            }
            g.disableScissor();
        }
        @Override public boolean isPauseScreen(){return false;}
    }
}
