package io.github.justhari.vanillavehicles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MechanicEntity extends Villager implements GeoEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int unlockedTiers;
    private static final JsonArray TIERS=loadTrades();
    public MechanicEntity(EntityType<? extends MechanicEntity> type,Level level){super(type,level);setVillagerData(new VillagerData(VillagerType.PLAINS,VillagerProfession.TOOLSMITH,1));}
    @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable("entity.bw_vv.mechanic");}
    public static void attributes(EntityAttributeCreationEvent event){event.put(VanillaVehicles.MECHANIC.get(),Villager.createAttributes().build());}
    @Override public void setVillagerData(VillagerData data){super.setVillagerData(data.setProfession(VillagerProfession.TOOLSMITH));}
    @Override protected void updateTrades(){
        if(offers==null)offers=new MerchantOffers();
        int index=0;
        for(var raw:TIERS){
            var tier=raw.getAsJsonObject();int bit=1<<index++;
            if((unlockedTiers&bit)!=0||getVillagerXp()<tier.get("total_exp_required").getAsInt())continue;
            for(var rawGroup:tier.getAsJsonArray("groups")){
                var group=rawGroup.getAsJsonObject();var trades=new ArrayList<JsonObject>();
                group.getAsJsonArray("trades").forEach(t->trades.add(t.getAsJsonObject()));
                Collections.shuffle(trades,new Random(random.nextLong()));
                int count=Math.min(group.get("num_to_select").getAsInt(),trades.size());
                for(int i=0;i<count;i++){
                    var trade=trades.get(i);var wants=trade.getAsJsonArray("wants");
                    var first=stack(wants.get(0).getAsJsonObject());
                    Optional<ItemCost> second=wants.size()>1?Optional.of(cost(wants.get(1).getAsJsonObject())):Optional.empty();
                    var result=stack(trade.getAsJsonArray("gives").get(0).getAsJsonObject());
                    offers.add(new MerchantOffer(new ItemCost(first.getItem(),first.getCount()),second,result,trade.get("max_uses").getAsInt(),trade.get("trader_exp").getAsInt(),.05f));
                }
            }
            unlockedTiers|=bit;
        }
    }
    private ItemStack stack(JsonObject data){
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.get("item").getAsString()));
        var quantity=data.get("quantity");int count=quantity.isJsonPrimitive()?quantity.getAsInt():random.nextIntBetweenInclusive(quantity.getAsJsonObject().get("min").getAsInt(),quantity.getAsJsonObject().get("max").getAsInt());
        return new ItemStack(item,count);
    }
    private ItemCost cost(JsonObject data){var s=stack(data);return new ItemCost(s.getItem(),s.getCount());}
    @Override public void notifyTrade(MerchantOffer offer){super.notifyTrade(offer);updateTrades();}
    @Override public void tick(){super.tick();if(!level().isClientSide&&tickCount%1200==0)restock();}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("UnlockedTiers",unlockedTiers);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);unlockedTiers=tag.getInt("UnlockedTiers");}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"base",0,state->state.setAndContinue(RawAnimation.begin().thenLoop("vehicle.idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    private static JsonArray loadTrades(){
        try(var stream=MechanicEntity.class.getResourceAsStream("/assets/bw_vv/mechanic-trades.json")){return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("tiers");}catch(IOException e){throw new UncheckedIOException(e);}
    }
}
