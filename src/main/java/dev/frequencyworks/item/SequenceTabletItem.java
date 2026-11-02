package dev.frequencyworks.item;

import dev.frequencyworks.sequencer.SequencerProgram;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Shift-right-click a target face to cycle link mode. Right-click air to
 * program this reusable nine-step tablet. Program travels with the item.
 */
public final class SequenceTabletItem extends Item {
    public SequenceTabletItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();
        if(player==null || !player.isShiftKeyDown())return InteractionResult.PASS;
        if(!context.getLevel().isClientSide() && player instanceof ServerPlayer server){
            ItemStack tablet=context.getItemInHand();
            SequencerProgram.cycleLink(tablet,server.level().dimension().location().toString(),
                    context.getClickedPos(),context.getClickedFace());
            List<SequencerProgram.Link> links=SequencerProgram.links(tablet);
            String status="Deselected";
            for(var link:links){
                if(link.pos().equals(context.getClickedPos()) && link.face()==context.getClickedFace()
                       && link.dimension().equals(server.level().dimension().location().toString())){
                    status=switch(link.mode()){case 1->"Input: Host to target";case 2->"Output: target to Host";default->"Both directions";};
                    break;
                }
            }
            server.displayClientMessage(Component.literal("Tablet face "+status),true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide() && player instanceof ServerPlayer server)
            dev.frequencyworks.network.SequencerNetworking.openTablet(server,hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide());
    }
}