package io.github.justhari.vanillavehicles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WindVane extends BaseEntityBlock {
    public static final IntegerProperty WIND=IntegerProperty.create("wind",0,3);
    public static final MapCodec<WindVane> CODEC=simpleCodec(WindVane::new);
    public WindVane(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(WIND,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(WIND);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Block.box(0,0,0,16,16,16);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Block.box(0,0,0,16,2,16);}
    @Override protected boolean isSignalSource(BlockState state){return true;}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){
        Direction output=switch(state.getValue(WIND)){case 0->Direction.EAST;case 1->Direction.SOUTH;case 2->Direction.WEST;default->Direction.NORTH;};
        return direction==output.getOpposite()?15:0;
    }
    @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return getSignal(state,level,pos,direction);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VaneEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,VanillaVehicles.VANE_ENTITY.get(),(world,pos,s,vane)->{
            if(world.getGameTime()%20!=0)return;int wind=WorldSystems.windIndex(world);
            if(s.getValue(WIND)!=wind){world.setBlock(pos,s.setValue(WIND,wind),3);world.updateNeighborsAt(pos,s.getBlock());}
        });
    }
    public static class VaneEntity extends BlockEntity implements GeoBlockEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public VaneEntity(BlockPos pos,BlockState state){super(VanillaVehicles.VANE_ENTITY.get(),pos,state);}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"base",0,state->state.setAndContinue(RawAnimation.begin().thenLoop("vehicle.idle"))));}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    }
}
