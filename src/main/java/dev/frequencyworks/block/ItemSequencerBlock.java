package dev.frequencyworks.block;

import dev.frequencyworks.blockentity.SequencerBlockEntity;
import dev.frequencyworks.registration.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A compact placed Sequencer. Only the inventory directly beneath is the Host.
 * The clean block drop comes from its loot table; installed tablets drop here.
 */
public final class ItemSequencerBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE=Block.box(4,0,4,12,10,12);
    public ItemSequencerBlock(Properties properties){super(properties.noOcclusion());}
    @Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,
                                              BlockPos pos, CollisionContext context){return SHAPE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,
                                              BlockPos pos, CollisionContext context){return SHAPE;}
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new SequencerBlockEntity(pos,state);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide()||type!=ModContent.SEQUENCER_ENTITY.get())return null;
        return (l,p,s,be)->{
            if(be instanceof SequencerBlockEntity seq)SequencerBlockEntity.serverTick(l,p,s,seq);
        };
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
                                                           Player player,BlockHitResult hit){
        if(!level.isClientSide() && player instanceof ServerPlayer server){
            if(level.getBlockEntity(pos) instanceof SequencerBlockEntity seq
                    && player.isShiftKeyDown() && seq.stopped()){
                seq.resume();
                server.displayClientMessage(Component.literal("Item Sequencer resumed."),true);
            } else {
                dev.frequencyworks.network.SequencerNetworking.openSequencer(server,pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
        if(!state.is(next.getBlock()) && !level.isClientSide()){
            if(level.getBlockEntity(pos) instanceof SequencerBlockEntity seq){
                Containers.dropContents(level,pos,seq);
                for(var refund:seq.takeRecovery())Block.popResource(level,pos,refund);
                level.updateNeighbourForOutputSignal(pos,this);
            }
        }
        super.onRemove(state,level,pos,next,moved);
    }
}