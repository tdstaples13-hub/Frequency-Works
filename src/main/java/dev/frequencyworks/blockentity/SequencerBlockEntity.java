package dev.frequencyworks.blockentity;

import dev.frequencyworks.registration.ModContent;
import dev.frequencyworks.item.SequenceTabletItem;
import dev.frequencyworks.sequencer.SequencerProgram;
import dev.frequencyworks.sequencer.SequencerProgram.Link;
import dev.frequencyworks.sequencer.SequencerProgram.Step;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Nine programmable tablet slots. Item transfer always goes through the item
 * handler directly below; there is no ordinary internal item inventory.
 * Runtime step/remaining/locked identity and emergency refunds survive restarts.
 */
public final class SequencerBlockEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> tablets=NonNullList.withSize(9, ItemStack.EMPTY);
    private final List<ItemStack> recovery=new ArrayList<>();
    private int tabletIndex, stepIndex, remaining;
    private ItemStack locked=ItemStack.EMPTY;
    private boolean stopped;
    private boolean edgeInitialized, rising, falling;
    private int lastPower;
    // One fake-player action at most every 10 game ticks, even when a single
    // instruction program automatically wraps back to its first step.
    private long nextInteractionAt;
    private String status="Ready";
    private Boolean hasProgramCache;
    // World-game-time scheduling survives unloads/restarts. A successful
    // transfer or completed instruction starts a fresh fast retry window.
    private boolean waiting, redstoneWait;
    private long firstWaitTick=-1L, retryAtTick;
    // The only expensive operations happen on schedule, not every tick.
    public boolean hasPartialTransfer(){return (remaining>0 && !locked.isEmpty()) || !recovery.isEmpty();}
    public boolean waiting(){return waiting;}
    public long retryAtTick(){return retryAtTick;}
    public boolean resetProgram(){
        // Caller must confirm partial progress before invoking this method.
        boolean partial=hasPartialTransfer();
        reset();
        stopped=false; waiting=false;redstoneWait=false;
        firstWaitTick=-1L;retryAtTick=0;
        status="Running";
        // The already-committed 10-tick fake-player cooldown is not bypassed.
        setChanged();
        return partial;
    }
    private void clearWait(){
        if(!waiting && firstWaitTick<0)return;
        waiting=false;redstoneWait=false;firstWaitTick=-1;retryAtTick=0;
        setChanged();
    }
    private void waitForRetry(ServerLevel server,String why){
        long now=server.getGameTime();
        if(firstWaitTick<0 || redstoneWait)firstWaitTick=now;
        waiting=true;redstoneWait=false;
        retryAtTick=dev.frequencyworks.core.SequencerRetrySchedule.nextAttempt(firstWaitTick,now);
        status=why;
        setChanged();
    }
    private void waitForRedstone(String why){
        // Unlike item interactions, redstone gates remain actively evaluated.
        if(!waiting || !redstoneWait){
            firstWaitTick=-1;retryAtTick=0;setChanged();
        }
        waiting=true;redstoneWait=true;status=why;
    }

    public SequencerBlockEntity(BlockPos pos,BlockState state) {
        super(ModContent.SEQUENCER_ENTITY.get(),pos,state);
    }
    public int tabletIndex(){return tabletIndex;}
    public int stepIndex(){return stepIndex;}
    public int remaining(){return remaining;}
    public boolean stopped(){return stopped;}
    public String status(){return status;}
    public void resume(){stopped=false;clearWait();status="Running";setChanged();}
    public void halt(){stopped=true;waiting=false;redstoneWait=false;status="Stopped";setChanged();}

    public int getContainerSize(){return tablets.size();}
    public boolean isEmpty(){for(ItemStack s:tablets)if(!s.isEmpty())return false;return true;}
    public ItemStack getItem(int i){return i>=0&&i<9?tablets.get(i):ItemStack.EMPTY;}
    public ItemStack removeItem(int i,int count){
        ItemStack taken=ContainerHelper.removeItem(tablets,i,count);
        if(!taken.isEmpty()){hasProgramCache=null;setChanged();}
        return taken;
    }
    public ItemStack removeItemNoUpdate(int i){
        ItemStack taken=ContainerHelper.takeItem(tablets,i);
        if(!taken.isEmpty()){hasProgramCache=null;setChanged();}
        return taken;
    }
    public void setItem(int i,ItemStack item){
        if(i<0||i>=9) return;
        if(!item.isEmpty() && !(item.getItem() instanceof SequenceTabletItem))return;
        tablets.set(i,item.isEmpty()?ItemStack.EMPTY:item.copyWithCount(1));
        hasProgramCache=null;
        if(tabletIndex==i){
            remaining=0;locked=ItemStack.EMPTY;edgeInitialized=false;
            clearWait();
        }
        setChanged();
    }
    public boolean canPlaceItem(int i,ItemStack item){return i>=0&&i<9&&item.getItem() instanceof SequenceTabletItem;}
    public boolean stillValid(Player player){
        return level!=null && level.getBlockEntity(worldPosition)==this
            && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;
    }
    public void clearContent(){
        for(int i=0;i<9;i++)tablets.set(i,ItemStack.EMPTY);
        reset();clearWait();hasProgramCache=null;setChanged();
    }
    private void reset(){tabletIndex=0;stepIndex=0;remaining=0;locked=ItemStack.EMPTY;edgeInitialized=false;rising=false;falling=false;}
    public List<ItemStack> takeRecovery(){
        List<ItemStack> result=new ArrayList<>(recovery); recovery.clear();setChanged();return result;
    }

    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);
        ContainerHelper.saveAllItems(tag,tablets,registries);
        tag.putInt("TabletIndex",tabletIndex);tag.putInt("StepIndex",stepIndex);
        tag.putInt("Remaining",remaining);tag.putBoolean("Stopped",stopped);
        if(!locked.isEmpty())tag.put("Locked",locked.save(registries));
        ListTag refunds=new ListTag();
        for(ItemStack s:recovery)if(!s.isEmpty())refunds.add(s.save(registries));
        tag.put("Recovery",refunds);
        tag.putBoolean("EdgeInitialized",edgeInitialized);
        tag.putInt("LastPower",lastPower);
        tag.putBoolean("Rising",rising);tag.putBoolean("Falling",falling);
        tag.putLong("NextInteractionAt",nextInteractionAt);
        tag.putBoolean("Waiting",waiting);
        tag.putBoolean("RedstoneWait",redstoneWait);
        tag.putLong("FirstWaitTick",firstWaitTick);
        tag.putLong("RetryAtTick",retryAtTick);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);
        tablets.clear();
        for(int i=0;i<9;i++)tablets.add(ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag,tablets,registries);
        for(int i=0;i<9;i++) if(!tablets.get(i).isEmpty()&&!(tablets.get(i).getItem() instanceof SequenceTabletItem))
            tablets.set(i,ItemStack.EMPTY);
        tabletIndex=Math.floorMod(tag.getInt("TabletIndex"),9);
        stepIndex=Math.floorMod(tag.getInt("StepIndex"),9);
        remaining=Math.max(0,Math.min(4096,tag.getInt("Remaining")));
        stopped=tag.getBoolean("Stopped");
        locked=ItemStack.parseOptional(registries,tag.getCompound("Locked"));
        if(!locked.isEmpty())locked=locked.copyWithCount(1);
        recovery.clear();
        for(Tag n:tag.getList("Recovery",Tag.TAG_COMPOUND)){
            if(n instanceof CompoundTag c) {
                ItemStack s=ItemStack.parseOptional(registries,c);
                if(!s.isEmpty())recovery.add(s);
            }
        }
        hasProgramCache=null;
        edgeInitialized=tag.getBoolean("EdgeInitialized");lastPower=tag.getInt("LastPower");
        rising=tag.getBoolean("Rising");falling=tag.getBoolean("Falling");
        nextInteractionAt=tag.getLong("NextInteractionAt");
        waiting=tag.getBoolean("Waiting");
        redstoneWait=tag.getBoolean("RedstoneWait");
        firstWaitTick=tag.contains("FirstWaitTick")?tag.getLong("FirstWaitTick"):-1L;
        retryAtTick=tag.getLong("RetryAtTick");
        if(stopped){waiting=false;redstoneWait=false;}
    }

    /** Lightweight server-to-client runtime status, not an inventory mirror. */
    public int runtime(int index){
        return switch(index){
            case 0->tabletIndex;case 1->stepIndex;
            case 2->remaining;case 3->stopped?1:0;
            case 4->waiting?1:0;case 5->hasPartialTransfer()?1:0;
            default->0;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SequencerBlockEntity seq){
        if(!(level instanceof ServerLevel server))return;
        seq.sampleEdges(server);
        if(server.getGameTime()%2==0 && !seq.stopped
                && (!seq.waiting || seq.redstoneWait || server.getGameTime()>=seq.retryAtTick))
            seq.process(server);
    }
    private void sampleEdges(ServerLevel server){
        ItemStack tablet=tablets.get(tabletIndex);
        if(tablet.isEmpty())return;
        Step current=SequencerProgram.step(tablet,stepIndex,server.registryAccess());
        if(!current.redstone())return;
        List<Link> links=SequencerProgram.links(tablet);
        if(current.link()<0||current.link()>=links.size())return;
        Link link=links.get(current.link());
        if(!targetValid(server,link))return;
        int signal=server.getSignal(link.pos(),link.face());
        if(edgeInitialized){
            if(lastPower==0 && signal>0)rising=true;
            if(lastPower>0 && signal==0)falling=true;
        }
        // A latched edge remains in memory until its gate advances; don't
        // dirty the chunk on every tick merely because the edge was latched.
        if(!edgeInitialized || signal!=lastPower)setChanged();
        edgeInitialized=true;lastPower=signal;
    }
    private boolean targetValid(ServerLevel server,Link link){
        return link.dimension().equals(server.dimension().location().toString())
            && worldPosition.distSqr(link.pos())<=16*16
            && server.hasChunkAt(link.pos());
    }

    private boolean hasProgram(ServerLevel server){
        if(hasProgramCache!=null)return hasProgramCache;
        for(ItemStack tablet:tablets)if(!tablet.isEmpty()){
            for(int i=0;i<9;i++)if(SequencerProgram.step(tablet,i,server.registryAccess()).configured()){
                hasProgramCache=true;return true;
            }
        }
        hasProgramCache=false;return false;
    }

    private void process(ServerLevel server){
        // Refunds from a non-transactional third-party handler take priority and
        // remain persisted until the Host can accept them.
        if(!recovery.isEmpty()){
            IItemHandler host=server.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.below(),Direction.UP);
            if(host==null)return;
            for(int i=recovery.size()-1;i>=0;i--){
                ItemStack rest=recovery.get(i);
                for(int s=0;s<host.getSlots()&&!rest.isEmpty();s++)
                    rest=host.insertItem(s,rest,false);
                if(rest.isEmpty())recovery.remove(i);else recovery.set(i,rest);
            }
            setChanged();
            if(!recovery.isEmpty()){status="Refund blocked";return;}
        }
        if(!hasProgram(server)){
            clearWait();status="No instructions";return;
        }
        int attempts=0,scanned=0;
        while(attempts<9 && scanned<81){
            scanned++;
            ItemStack tablet=tablets.get(tabletIndex);
            if(tablet.isEmpty()){
                advance();continue;
            }
            Step step=SequencerProgram.step(tablet,stepIndex,server.registryAccess());
            if(!step.configured()){advance();continue;}
            attempts++;
            if(remaining<=0)remaining=step.amount();
            List<Link> links=SequencerProgram.links(tablet);
            if(step.link()<0||step.link()>=links.size()){
                if(!failure(tablet,"Step has no linked face"))return;
                continue;
            }
            Link link=links.get(step.link());
            if(!targetValid(server,link)){
                // Preserve links across unloads. Never force-load a target.
                waitForRetry(server,"Target unavailable");return;
            }
            if(step.redstone()){
                boolean pass=switch(step.gate()){
                    case 1->lastPower==0;
                    case 2->rising;
                    case 3->falling;
                    default->step.gate()>=4 ? lastPower>=step.gate()-3 : lastPower>0;
                };
                if(!edgeInitialized) {waitForRedstone("Reading redstone");return;}
                if(!pass){waitForRedstone("Waiting for signal");return;}
                advance();continue;
            }
            if(step.interaction()){
                if(server.getGameTime()<nextInteractionAt) {
                    // Expected safety cooldown is not a failed operation.
                    status="Waiting for interaction cooldown";
                    return;
                }
                var result=dev.frequencyworks.sequencer.SequencerInteractionService.perform(
                        server,link,step.gate());
                if(result!=dev.frequencyworks.sequencer.SequencerInteractionService.Outcome.SUCCESS){
                    waitForRetry(server,"Interaction blocked or rejected");
                    return;
                }
                nextInteractionAt=server.getGameTime()+10;
                status="Interaction executed";
                advance();
                continue;
            }
            if(!link.permits(step.receive())){
                if(!failure(tablet,"Face direction unavailable"))return;
                continue;
            }
            IItemHandler host=server.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.below(),Direction.UP);
            IItemHandler target=server.getCapability(Capabilities.ItemHandler.BLOCK,link.pos(),link.face());
            if(host==null || target==null){
                waitForRetry(server,"Waiting for inventory");
                return;
            }
            IItemHandler from=step.receive()?target:host;
            IItemHandler to=step.receive()?host:target;
            Move move=moveOne(from,to,step,Math.min(64,remaining),server);
            if(move.count()<=0){
                waitForRetry(server,"Waiting for items/space");
                return;
            }
            // Any progress restores the fast retry window; a later blockage
            // starts counting its own first minute from that point.
            clearWait();
            if(locked.isEmpty())locked=move.prototype().copyWithCount(1);
            remaining-=move.count();
            status="Moving items";
            setChanged();
            if(remaining<=0)advance();
        }
    }
    private boolean failure(ItemStack tablet,String why){
        status=why;
        int policy=SequencerProgram.failurePolicy(tablet);
        if(policy==1 && remaining==SequencerProgram.step(tablet,stepIndex,level.registryAccess()).amount()){
            advance();return true;
        }
        if(policy==2){halt();return false;}
        if(level instanceof ServerLevel server)waitForRetry(server,why);
        return false;
    }
    private void advance(){
        clearWait();
        remaining=0;locked=ItemStack.EMPTY;
        edgeInitialized=false;rising=false;falling=false;lastPower=0;
        stepIndex++;
        if(stepIndex>=9){stepIndex=0;tabletIndex=(tabletIndex+1)%9;}
        setChanged();
    }

    private record Move(int count,ItemStack prototype){}
    private Move moveOne(IItemHandler source,IItemHandler destination,Step step,int limit,ServerLevel server){
        for(int i=0;i<Math.min(source.getSlots(),128);i++){
            ItemStack sample=source.extractItem(i,limit,true);
            if(sample.isEmpty()||!matches(sample,step))continue;
            if(!locked.isEmpty()&&!ItemStack.isSameItemSameComponents(sample,locked))continue;
            for(int j=0;j<Math.min(destination.getSlots(),128);j++){
                ItemStack remainder=destination.insertItem(j,sample.copy(),true);
                int can=Math.max(0,sample.getCount()-remainder.getCount());
                if(can<=0)continue;
                ItemStack actual=source.extractItem(i,can,false);
                if(actual.isEmpty())continue;
                if(!ItemStack.isSameItemSameComponents(actual,sample)){
                    refund(source,i,actual);continue;
                }
                ItemStack leftover=destination.insertItem(j,actual.copy(),false);
                int sent=actual.getCount()-leftover.getCount();
                if(!leftover.isEmpty())refund(source,i,leftover);
                if(sent>0)return new Move(sent,actual.copyWithCount(1));
            }
        }
        return new Move(0,ItemStack.EMPTY);
    }
    private void refund(IItemHandler source,int slot,ItemStack item){
        ItemStack remaining=source.insertItem(slot,item.copy(),false);
        if(!remaining.isEmpty()){
            // Persist a bounded emergency refund instead of destroying items.
            recovery.add(remaining.copy());setChanged();
        }
    }
    private boolean matches(ItemStack sample,Step step){
        boolean match;
        if(!step.item().isEmpty())match=ItemStack.isSameItemSameComponents(sample,step.item());
        else if(!step.tag().isEmpty()){
            ResourceLocation id=ResourceLocation.tryParse(step.tag());
            match=id!=null && sample.is(TagKey.create(Registries.ITEM,id));
        } else match=false;
        return step.blacklist()?!match:match;
    }
}