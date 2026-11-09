package io.github.justhari.vanillavehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class VehicleItem extends Item {
    public final String id,vehicle;
    public VehicleItem(Properties properties,String id,String vehicle){super(properties);this.id=id;this.vehicle=vehicle;}
    @Override public InteractionResult useOn(UseOnContext context){
        if(id.equals("redstone_compressor")||id.startsWith("smoke_bomb.")){
            if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
            boolean smoke=id.startsWith("smoke_bomb.");
            var utility=(smoke?VanillaVehicles.SMOKE:VanillaVehicles.COMPRESSOR).get().create(context.getLevel());
            var block=context.getClickedPos().relative(context.getClickedFace());utility.setPos(block.getX()+.5,block.getY(),block.getZ()+.5);
            if(smoke)utility.setColor(DyeColor.byName(id.substring(11),DyeColor.WHITE));
            if(!context.getLevel().noCollision(utility))return InteractionResult.FAIL;
            context.getLevel().addFreshEntity(utility);if(context.getPlayer()==null||!context.getPlayer().getAbilities().instabuild)context.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }
        if(vehicle==null)return super.useOn(context);
        VehicleKind kind;
        try{kind=VehicleKind.named(vehicle);}catch(IllegalArgumentException e){return InteractionResult.PASS;}
        Level level=context.getLevel();
        if(level.isClientSide)return InteractionResult.SUCCESS;
        BlockPos block=context.getClickedPos().relative(context.getClickedFace());
        var entity=VanillaVehicles.VEHICLES.get(kind).get().create(level);
        if(entity==null)return InteractionResult.FAIL;
        entity.setPos(block.getX()+.5,block.getY()+.05,block.getZ()+.5);
        if(context.getPlayer()!=null)entity.setYRot(context.getPlayer().getYRot());
        if(id.startsWith("logcar.")||id.startsWith("plane."))entity.setVariant("wood",id.split("\\.",2)[1]);
        if(!level.noCollision(entity,entity.getBoundingBox()))return InteractionResult.FAIL;
        level.addFreshEntity(entity);
        if(context.getPlayer()==null||!context.getPlayer().getAbilities().instabuild)context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(id.equals("guide_book")){
            if(level.isClientSide&&FMLEnvironment.dist==Dist.CLIENT)VehicleClient.openGuide();
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        return super.use(level,player,hand);
    }
}
