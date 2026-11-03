package dev.frequencyworks.network;

import dev.frequencyworks.FrequencyWorks;
import dev.frequencyworks.blockentity.SequencerBlockEntity;
import dev.frequencyworks.item.SequenceTabletItem;
import dev.frequencyworks.menu.SequencerMenu;
import dev.frequencyworks.menu.TabletMenu;
import dev.frequencyworks.sequencer.SequencerProgram;
import dev.frequencyworks.sequencer.SequencerProgram.Step;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** All programmable Tablet edits are server-authoritative. Client sends only
 * an action and step index; the ghost stack comes from the server menu cursor.
 */
public final class SequencerNetworking {
    private SequencerNetworking(){}
    public record TabletState(int containerId,CompoundTag program) implements CustomPacketPayload {
        public static final Type<TabletState> TYPE=new Type<>(
                ResourceLocation.fromNamespaceAndPath(FrequencyWorks.MOD_ID,"tablet_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,TabletState> STREAM_CODEC=StreamCodec.of(
                (b,p)->{b.writeVarInt(p.containerId);b.writeNbt(p.program);},
                b->new TabletState(b.readVarInt(),b.readNbt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record TabletAction(int hand,String action,int step,int value) implements CustomPacketPayload {
        public static final Type<TabletAction> TYPE=new Type<>(
                ResourceLocation.fromNamespaceAndPath(FrequencyWorks.MOD_ID,"tablet_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,TabletAction> STREAM_CODEC=StreamCodec.of(
                (b,p)->{b.writeVarInt(p.hand);b.writeUtf(p.action,24);b.writeVarInt(p.step);b.writeVarInt(p.value);},
                b->new TabletAction(b.readVarInt(),b.readUtf(24),b.readVarInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(PayloadRegistrar registrar){
        registrar.playToClient(TabletState.TYPE,TabletState.STREAM_CODEC,(payload,context)->{
            if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                dev.frequencyworks.client.SequencerClient.receive(payload);
        });
        registrar.playToServer(TabletAction.TYPE,TabletAction.STREAM_CODEC,SequencerNetworking::handle);
    }
    public static void openTablet(ServerPlayer player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(!(held.getItem() instanceof SequenceTabletItem))return;
        int index=hand==InteractionHand.MAIN_HAND?0:1;
        player.openMenu(new SimpleMenuProvider(
            (id,inventory,viewer)->new TabletMenu(id,inventory,index),
            Component.literal("Sequence Tablet")
        ),buf->buf.writeVarInt(index));
        if(player.containerMenu instanceof TabletMenu menu)
            sendState(player,menu,held);
    }
    public static void openSequencer(ServerPlayer player,BlockPos pos){
        if(!(player.serverLevel().getBlockEntity(pos) instanceof SequencerBlockEntity be))return;
        if(!be.stillValid(player))return;
        player.openMenu(new SimpleMenuProvider(
            (id,inventory,viewer)->new SequencerMenu(id,inventory,pos,be),
            Component.literal("Item Sequencer")
        ),buf->buf.writeBlockPos(pos));
    }
    private static void sendState(ServerPlayer player,TabletMenu menu,ItemStack held){
        PacketDistributor.sendToPlayer(player,new TabletState(menu.containerId,SequencerProgram.program(held)));
    }
    private static void handle(TabletAction req,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        if(!(player.containerMenu instanceof TabletMenu menu)||menu.hand()!=req.hand())return;
        if(req.hand()<0||req.hand()>1)return;
        ItemStack held=player.getItemInHand(req.hand()==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        if(!(held.getItem() instanceof SequenceTabletItem)||!menu.stillValid(player))return;
        int slot=req.step();
        if(slot<0||slot>=9)return;
        if(req.action().equals("failure")){
            SequencerProgram.cycleFailurePolicy(held);
        } else {
            Step s=SequencerProgram.step(held,slot,player.registryAccess());
            Step next=s;
            switch(req.action()){
                case "set" -> {
                    ItemStack cursor=menu.getCarried();
                    if(!cursor.isEmpty()) {
                        if(cursor.getItem() instanceof dev.frequencyworks.item.TagCardItem
                                && dev.frequencyworks.item.TagCardItem.tag(cursor).isEmpty())return;
                        next=SequencerProgram.withItem(s,cursor);
                    }
                }
                case "clear" -> next=SequencerProgram.clearItem(s);
                case "option" -> next=s.interaction()
                        ?SequencerProgram.nextInteractionAction(s)
                        :s.redstone()?SequencerProgram.withGate(s,(s.gate()+1)%19)
                        :SequencerProgram.nextFaceOption(held,s);
                case "card_target" -> {
                    if(!s.interaction() && !s.redstone())return;
                    next=SequencerProgram.nextInteractionTarget(held,s);
                }
                case "interaction_target" -> {
                    if(!s.interaction())return;
                    next=SequencerProgram.nextInteractionTarget(held,s);
                }
                case "link" -> {
                    int size=SequencerProgram.links(held).size();
                    int index=s.link()+1;
                    if(index>=size)index=-1;
                    next=SequencerProgram.withLink(s,index);
                }
                case "direction" -> next=SequencerProgram.withReceive(s,!s.receive());
                case "amount" -> next=SequencerProgram.withAmount(s,Math.max(1,Math.min(4096,s.amount()+req.value())));
                case "blacklist" -> {
                    if(s.interaction() || s.redstone())return;
                    next=SequencerProgram.withBlacklist(s,!s.blacklist());
                }
                case "gate" -> next=SequencerProgram.withGate(s,(s.gate()+1)%19);
                default -> {return;}
            }
            SequencerProgram.setStep(held,slot,next,player.registryAccess());
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        sendState(player,menu,held);
    }
}