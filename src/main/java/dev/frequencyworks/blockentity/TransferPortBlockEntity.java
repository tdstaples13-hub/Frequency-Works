package dev.frequencyworks.blockentity;

import dev.frequencyworks.block.TransferPortBlock;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.core.PortRates;
import dev.frequencyworks.registration.ModContent;
import dev.frequencyworks.world.FrequencyNbt;
import dev.frequencyworks.world.FrequencySavedData;
import dev.frequencyworks.world.PortTransferService;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Autonomous, loaded-only endpoint. All block entity settings vanish on break.
 * No custom data is ever copied to the dropped Transfer Port item. */
public final class TransferPortBlockEntity extends BlockEntity {
    public enum FaceMode { NONE, INPUT, OUTPUT, BOTH }
    private FrequencyId binding;
    private UUID configuredBy;
    private final FaceMode[] faces = new FaceMode[6];
    private boolean stackMode = true;
    private final PortFilterBank inputFilter = new PortFilterBank();
    private final PortFilterBank outputFilter = new PortFilterBank();
    private final PortRates.ItemBudget itemBudget = new PortRates.ItemBudget();
    private final PortRates.StackBudget stackBudget = new PortRates.StackBudget();
    private final int[] slotCursors = new int[6];
    private int faceCursor;
    private long retryAt;
    private BlockCapabilityCache<IItemHandler, Direction>[] caches;

    public TransferPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.PORT_ENTITY.get(), pos, state);
        java.util.Arrays.fill(faces, FaceMode.NONE);
    }

    public FrequencyId binding() { return binding; }
    public boolean stackMode() { return stackMode; }
    public FaceMode faceMode(Direction side) { return faces[side.ordinal()]; }
    public PortFilterBank inputFilter() { return inputFilter; }
    public PortFilterBank outputFilter() { return outputFilter; }
    public int filterCapacity() {
        // Three rows per bank, expanding one column per tier.
        int[] counts = {3,6,9,12,15};
        return counts[((TransferPortBlock)getBlockState().getBlock()).tierIndex()];
    }

    /** Both directions are selectable even before filters are configured.
     * When a face is in BOTH mode, export excludes anything import-eligible
     * on that face, preventing a simple host<->frequency feedback loop. */
    public boolean setFilterRule(boolean incoming, int slot, String rule, UUID actor) {
        if (!canConfigure(actor)) return false;
        PortFilterBank bank = incoming ? inputFilter : outputFilter;
        if (!bank.put(slot, rule, filterCapacity())) return false;
        retryAt = 0; setChanged(); return true;
    }
    public boolean setFilterWhitelist(boolean incoming, boolean whitelist, UUID actor) {
        if (!canConfigure(actor)) return false;
        PortFilterBank bank = incoming ? inputFilter : outputFilter;
        bank.whitelist(whitelist);
        retryAt = 0; setChanged(); return true;
    }

    public boolean assign(FrequencyId id, UUID actor) {
        if (binding != null || !(level instanceof ServerLevel server) || !id.accessibleBy(actor)
                || FrequencySavedData.get(server).portAccess(id, actor).isEmpty()) return false;
        binding = id;
        configuredBy = actor;
        setChanged();
        return true;
    }

    public boolean setMode(Direction side, FaceMode mode, UUID actor) {
        if (binding == null || !canConfigure(actor)) return false;
        faces[side.ordinal()] = mode;
        retryAt = 0;
        setChanged();
        return true;
    }

    public boolean setStackMode(boolean stack, UUID actor) {
        if (!canConfigure(actor)) return false;
        stackMode = stack;
        retryAt = 0;
        setChanged();
        return true;
    }

    public boolean canConfigure(UUID actor) {
        return binding != null && (binding.scope() == FrequencyId.Scope.PUBLIC
                ? actor.equals(configuredBy) : actor.equals(binding.owner()));
    }

    public String status() {
        if (binding == null) return "Transfer Port: unconfigured";
        return "Transfer Port " + binding.displayNumber() + " (" + binding.scope() + ", "
                + (stackMode ? "stack" : "individual") + ")";
    }

    @SuppressWarnings("unchecked")
    private void initCaches(ServerLevel server, BlockPos targetPos) {
        if (caches != null) return;
        caches = (BlockCapabilityCache<IItemHandler, Direction>[]) new BlockCapabilityCache<?, ?>[6];
        for (Direction side : Direction.values()) {
            caches[side.ordinal()] = BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, server, targetPos, side);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TransferPortBlockEntity port) {
        if (!(level instanceof ServerLevel server) || port.binding == null) return;
        long now = level.getGameTime();
        if (now < port.retryAt) return;
        // Cache the configured face indices for this tick. Reuse the same face for
        // multiple operations: one machine face must be able to reach 4 stacks/tick.
        int[] enabledFaces = new int[6];
        int faceCount = 0;
        for (int i = 0; i < 6; i++) {
            if (port.faces[i] != FaceMode.NONE) enabledFaces[faceCount++] = i;
        }
        if (faceCount == 0) return;
        var data = FrequencySavedData.get(server);
        var network = data.portAccess(port.binding, port.configuredBy).orElse(null);
        if (network == null) return;
        int tier = ((TransferPortBlock) state.getBlock()).tierIndex();
        int limit = port.stackMode ? port.stackBudget.next(tier) : port.itemBudget.next(tier);
        if (limit <= 0) return;
        BlockPos target = pos.relative(state.getValue(TransferPortBlock.FACING));
        if (!server.hasChunkAt(target)) return;
        port.initCaches(server, target);

        boolean moved = false;
        int remainingItems = port.stackMode ? 0 : limit;
        int attempts = port.stackMode ? Math.min(4, limit) : 4;
        for (int step = 0; step < attempts; step++) {
            int dir = enabledFaces[(port.faceCursor + step) % faceCount];
            FaceMode mode = port.faces[dir];
            IItemHandler handler = port.caches[dir].getCapability();
            if (handler == null) continue;
            int maxCount = port.stackMode ? 64 : remainingItems;
            if (maxCount <= 0) break;
            int transfer = 0;
            if (mode == FaceMode.INPUT || mode == FaceMode.BOTH) {
                transfer = PortTransferService.pullFromMachine(handler, network, maxCount,
                        port.slotCursors[dir], port.inputFilter);
            }
            if (transfer == 0 && (mode == FaceMode.OUTPUT || mode == FaceMode.BOTH)) {
                // BOTH must not export an item the same face would immediately
                // re-import; separate one-way faces keep their configured filters.
                java.util.function.Predicate<net.minecraft.world.item.ItemStack> allowed =
                    mode == FaceMode.BOTH
                        ? item -> port.outputFilter.test(item) && !port.inputFilter.test(item)
                        : port.outputFilter;
                transfer = PortTransferService.pushToMachine(handler, network, maxCount,
                        port.slotCursors[dir], allowed);
            }
            port.slotCursors[dir]++; // no systematic preference for slot zero
            if (transfer > 0) {
                moved = true;
                data.changed();
                if (!port.stackMode) remainingItems -= transfer;
            }
        }
        port.faceCursor = (port.faceCursor + attempts) % faceCount;
        if (!moved) port.retryAt = now + 10; // inexpensive idle backoff
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (binding != null) tag.put("Binding", FrequencyNbt.encode(binding));
        if (configuredBy != null) tag.putUUID("Configurer", configuredBy);
        tag.putBoolean("StackMode", stackMode);
        tag.put("InputFilter", inputFilter.save());
        tag.put("OutputFilter", outputFilter.save());
        int[] modes = new int[6];
        for (int i = 0; i < 6; i++) modes[i] = faces[i].ordinal();
        tag.putIntArray("Faces", modes);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try { binding = tag.contains("Binding") ? FrequencyNbt.decode(tag.getCompound("Binding")) : null; }
        catch (IllegalArgumentException ex) { binding = null; }
        configuredBy = tag.hasUUID("Configurer") ? tag.getUUID("Configurer") : null;
        if (binding != null && configuredBy == null) binding = null;
        stackMode = !tag.contains("StackMode") || tag.getBoolean("StackMode");
        inputFilter.load(tag.getCompound("InputFilter"), filterCapacity());
        outputFilter.load(tag.getCompound("OutputFilter"), filterCapacity());
        int[] modes = tag.getIntArray("Faces");
        for (int i = 0; i < 6; i++) {
            faces[i] = i < modes.length && modes[i] >= 0 && modes[i] < FaceMode.values().length
                    ? FaceMode.values()[modes[i]] : FaceMode.NONE;
        }
    }
}
