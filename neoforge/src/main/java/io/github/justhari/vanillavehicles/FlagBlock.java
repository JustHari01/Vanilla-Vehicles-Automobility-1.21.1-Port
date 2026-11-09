package io.github.justhari.vanillavehicles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Original coloured flags and stackable poles; flags turn with persistent world wind. */
public class FlagBlock extends BaseEntityBlock {
    public static final MapCodec<FlagBlock> CODEC=simpleCodec(p->new FlagBlock(p,"white"));
    public final String color;
    public FlagBlock(Properties properties,String color){super(properties);this.color=color;registerDefaultState(stateDefinition.any().setValue(WindVane.WIND,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(WindVane.WIND);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Block.box(6,0,6,10,16,10);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Block.box(7,0,7,9,16,9);}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||level.getBlockState(pos.below()).getBlock() instanceof FlagBlock;}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){return canSurvive(state,level,pos)?super.updateShape(state,direction,neighbor,level,pos,other):Blocks.AIR.defaultBlockState();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FlagEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,VanillaVehicles.FLAG_ENTITY.get(),(world,pos,s,flag)->{
        if(world.getGameTime()%20==0){int wind=WorldSystems.windIndex(world);if(s.getValue(WindVane.WIND)!=wind)world.setBlock(pos,s.setValue(WindVane.WIND,wind),3);}
    });}
    public static class FlagEntity extends BlockEntity implements GeoBlockEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public FlagEntity(BlockPos pos,BlockState state){super(VanillaVehicles.FLAG_ENTITY.get(),pos,state);}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"base",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("vehicle.idle"))));}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    }
}
