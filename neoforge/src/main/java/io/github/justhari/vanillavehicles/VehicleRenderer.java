package io.github.justhari.vanillavehicles;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.animation.AnimationState;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class VehicleRenderer extends GeoEntityRenderer<VehicleEntity> {
    private String texturePass="";
    public VehicleRenderer(EntityRendererProvider.Context ctx){super(ctx,new Model());shadowRadius=.8f;
        addRenderLayer(new GeoRenderLayer<>(this){
            @Override public void render(PoseStack pose,VehicleEntity v,BakedGeoModel model,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
                var textures=VanillaVehicles.CATALOG.getAsJsonObject("vehicles").getAsJsonObject(v.kind.id).getAsJsonObject("textures");
                String key="overlay_"+v.variant("sticker");
                if(v.variant("sticker").isEmpty()||!textures.has(key))return;
                var rt=RenderType.entityCutoutNoCull(VanillaVehicles.rl(textures.get(key).getAsString()+".png"));
                getRenderer().reRender(model,pose,buffers,v,rt,buffers.getBuffer(rt),partial,light,overlay,0xffffffff);
            }
        });
        addRenderLayer(new GeoRenderLayer<>(this){
            @Override public void render(PoseStack pose,VehicleEntity v,BakedGeoModel model,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,float partial,int light,int overlay){
                var sheets=VanillaVehicles.CATALOG.getAsJsonObject("vehicles").getAsJsonObject(v.kind.id).getAsJsonObject("sheets");
                for(String key:sheets.keySet()){
                    if(key.equals("aura_animated"))continue;
                    if(!key.equals("wings_animated")&&v.fuel()<=0)continue;
                    int frames=sheets.get(key).getAsInt();int frame=key.equals("wings_animated")?Math.floorMod((int)(v.renderDistance()*.9),frames):Math.floorMod((int)((v.tickCount+partial)*(v.kind==VehicleKind.PRISMER?.3:v.kind==VehicleKind.MECH||v.kind==VehicleKind.STEAMBOAT?.4:.5)),frames);
                    var texture=VanillaVehicles.rl("textures/derived/"+v.kind.id+"/"+key+"/"+frame+".png");var rt=RenderType.entityCutoutNoCull(texture);
                    texturePass=key;
                    try{getRenderer().reRender(model,pose,buffers,v,rt,buffers.getBuffer(rt),partial,key.equals("wings_animated")||key.equals("tentacles_animated")?light:LightTexture.FULL_BRIGHT,overlay,0xffffffff);}finally{texturePass="";}
                }
                if(v.kind==VehicleKind.PRISMER&&v.fuel()>0){
                    var aura=getGeoModel().getBakedModel(VanillaVehicles.rl("geo/prismer_aura.geo.json"));
                    aura.getBone("discs").ifPresent(b->b.setRotY((v.tickCount+partial)*.04f));
                    var rt=RenderType.entityTranslucentEmissive(VanillaVehicles.rl("textures/derived/prismer/aura_animated/"+Math.floorMod((int)((v.tickCount+partial)*.8),18)+".png"));
                    pose.pushPose();pose.scale(1.4f,1.4f,1.4f);texturePass="aura";
                    try{getRenderer().reRender(aura,pose,buffers,v,rt,buffers.getBuffer(rt),partial,LightTexture.FULL_BRIGHT,overlay,0xaaffffff);}finally{texturePass="";pose.popPose();}
                }
            }
        });
    }
    private boolean colorPart(VehicleEntity v,GeoBone bone){
        String name=bone.getName().toLowerCase();
        return switch(v.kind){
            case MINECAR,MOBTRUCK -> name.equals("body")||name.startsWith("defender")||name.startsWith("defefnder")||name.equals("colorchange");
            case PLANE -> name.equals("wingldyeable")||name.equals("wingrdyeable")||name.equals("chair")||name.equals("flag_dyeable");
            case BALLOON -> name.equals("balloon1")||name.equals("balloon2");
            case STEAMBOAT -> name.equals("wool_cover");
            case MECH -> name.startsWith("sprayable")||name.equals("metalpartl")||name.equals("metalpartr")||name.equals("foot_l_piston")||name.equals("foot_r_piston");
            default -> false;
        };
    }
    @Override public void renderRecursively(PoseStack pose,VehicleEntity v,GeoBone bone,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,boolean reRender,float partial,int light,int overlay,int color){
        int tint=(!reRender||texturePass.equals("wings_animated"))&&v.color()>=0&&colorPart(v,bone)?0xff000000|net.minecraft.world.item.DyeColor.byId(v.color()).getTextureDiffuseColor():color;
        String name=bone.getName().toLowerCase();
        int illumination=name.contains("light")||name.equals("lantern")||name.equals("fuel_glow")&&v.fuel()>0?LightTexture.FULL_BRIGHT:light;
        boolean oldHidden=bone.isHidden();
        if(!texturePass.isEmpty()&&!texturePass.equals("aura")&&!texturePass.equals("tentacles_animated")){
            boolean visible=texturePass.equals("wings_animated")?name.equals("wingldyeable")||name.equals("wingrdyeable"):switch(v.kind){
                case MINECAR,LOGCAR -> name.equals("furnace")||name.equals("fuel_glow");
                case MECH -> name.equals("upper_part");
                case MOBTRUCK,PLANE -> name.equals("body");
                case STEAMBOAT -> name.equals("furnace");
                default -> true;
            };
            if(!visible)bone.setHidden(true);
        }
        try{super.renderRecursively(pose,v,bone,type,buffers,buffer,reRender,partial,illumination,overlay,tint);}finally{bone.setHidden(oldHidden);}
    }
    private static class Model extends GeoModel<VehicleEntity> {
        @Override public ResourceLocation getModelResource(VehicleEntity v){return VanillaVehicles.rl("geo/"+v.kind.id+".geo.json");}
        @Override public ResourceLocation getTextureResource(VehicleEntity v){
            var textures=VanillaVehicles.CATALOG.getAsJsonObject("vehicles").getAsJsonObject(v.kind.id).getAsJsonObject("textures");
            String key=textures.has("default")?"default":textures.has("oak")?"oak":textures.keySet().iterator().next();
            if(v.kind==VehicleKind.BALLOON)key=v.variant("envelope").isEmpty()?"empty":"envelope."+v.variant("envelope");
            if(v.kind==VehicleKind.PRISMER)key=v.fuel()>0?"active":"inactive";
            return VanillaVehicles.rl(textures.get(key).getAsString()+".png");
        }
        @Override public ResourceLocation getAnimationResource(VehicleEntity v){return VanillaVehicles.rl("animations/vehicle.animation.json");}
        @Override public void setCustomAnimations(VehicleEntity v,long id,AnimationState<VehicleEntity> state){
            super.setCustomAnimations(v,id,state);
            float motion=(float)v.renderDistance()+v.renderSpeed()*state.getPartialTick();
            for(var bone:getAnimationProcessor().getRegisteredBones()){
                String name=bone.getName().toLowerCase();
                var initial=bone.getInitialSnapshot();bone.setHidden(false);bone.setChildrenHidden(false);
                if(name.startsWith("wheel"))bone.setRotX(motion*4);
                if(name.contains("propeller"))bone.setRotZ(motion*20);
                if(name.equals("all_vertical_steer")&&v.kind==VehicleKind.PLANE)bone.setRotX(-v.getXRot()*Mth.DEG_TO_RAD);
                if(name.equals("all_horizontal_steer")&&v.kind==VehicleKind.PLANE)bone.setRotZ((v.roll(state.getPartialTick())-Mth.clamp(v.steer(),-60,60)*.6f)*Mth.DEG_TO_RAD);
                if(name.equals("drivingwheel"))bone.setRotZ(initial.getRotZ()-Mth.clamp(v.steer()*1.4f,-60,60)*Mth.DEG_TO_RAD);
                if(name.equals("fuel_indicator")){bone.updateScale(1,1,1);if(v.kind==VehicleKind.PLANE||v.kind==VehicleKind.MOBTRUCK)bone.setScaleX((10-v.fuel())/10);else bone.setScaleY((10-v.fuel())/10);}
                if(name.equals("boost_indicator"))bone.setScaleX(1-v.boostCooldown()/140f);
                if(name.equals("charge_indicator"))bone.setScaleY(1-v.weaponCooldown()/60f);
                if(name.equals("backdoor_open"))bone.setHidden(!v.flag("trunk_open"));
                if(name.equals("backdoor_closed"))bone.setHidden(v.flag("trunk_open"));
                if(name.equals("flag")){bone.setHidden(true);bone.setChildrenHidden(true);}
                if(name.contains("chest")||name.contains("bundle"))bone.setHidden(!v.flag("chest"));
                if(name.equals("bell")||name.startsWith("bell_"))bone.setHidden(!v.flag("bell"));
                if(name.equals("envelope")){boolean hide=v.variant("envelope").isEmpty();bone.setHidden(hide);bone.setChildrenHidden(hide);}
            }
        }
    }
}
