package dev.frequencyworks.network;

import dev.frequencyworks.FrequencyWorks;
import dev.frequencyworks.block.TransferBufferBlock;
import dev.frequencyworks.blockentity.TransferBufferBlockEntity;
import dev.frequencyworks.blockentity.TransferPortBlockEntity;
import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.core.NetworkInventory;
import dev.frequencyworks.item.TransferBufferItem;
import dev.frequencyworks.item.TagCardItem;
import dev.frequencyworks.world.FrequencySavedData;
import dev.frequencyworks.world.BufferInventoryActions;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.UUID;

/** Configuration packets are intentionally server-authoritative. Never accept an
 * owner UUID, a remote coordinate, or an inventory from a client as proof of access.
 */
public final class ConfigNetworking {
    private ConfigNetworking() {}

    public record OpenConfig(String kind, BlockPos pos, int hand, CompoundTag state) implements CustomPacketPayload {
        public static final Type<OpenConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FrequencyWorks.MOD_ID, "config_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfig> STREAM_CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.kind, 12); b.writeBlockPos(p.pos); b.writeVarInt(p.hand + 1); b.writeNbt(p.state); },
                b -> new OpenConfig(b.readUtf(12), b.readBlockPos(), b.readVarInt() - 1, b.readNbt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Action(BlockPos pos, int hand, String action, String value, int slot) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FrequencyWorks.MOD_ID, "config_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of(
                (b, p) -> { b.writeBlockPos(p.pos); b.writeVarInt(p.hand + 1); b.writeUtf(p.action, 32); b.writeUtf(p.value, 160); b.writeVarInt(p.slot); },
                b -> new Action(b.readBlockPos(), b.readVarInt() - 1, b.readUtf(32), b.readUtf(160), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(OpenConfig.TYPE, OpenConfig.STREAM_CODEC,
                (payload, context) -> clientOpen(payload));
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, ConfigNetworking::handleAction);
    }

    /** Invoked on client only; keeps client-only Minecraft classes out of common classes. */
    private static void clientOpen(OpenConfig packet) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            dev.frequencyworks.client.ConfigClient.open(packet);
    }

    public static void openBuffer(ServerPlayer player, BlockPos pos) {
        if (!near(player, pos)) return;
        if (!(player.serverLevel().getBlockEntity(pos) instanceof TransferBufferBlockEntity buffer)) return;
        showBuffer(player, pos, -1, buffer.binding(), ((TransferBufferBlock)buffer.getBlockState().getBlock()).tier(), "", false, "");
    }
    public static void openHeldBuffer(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof TransferBufferItem buffer)) return;
        showBuffer(player, BlockPos.ZERO, hand == InteractionHand.MAIN_HAND ? 0 : 1,
                TransferBufferItem.binding(stack), buffer.tier(), "", false, "");
    }
    public static void openHeldTag(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof TagCardItem)
            showTag(player, hand == InteractionHand.MAIN_HAND ? 0 : 1, TagCardItem.tag(stack), "");
    }
    private static void showTag(ServerPlayer player, int hand, String selected, String warning) {
        CompoundTag state = new CompoundTag();
        state.putBoolean("Assigned", true);
        state.putString("Tag", selected);
        state.putString("Warning", warning);
        PacketDistributor.sendToPlayer(player, new OpenConfig("tag", BlockPos.ZERO, hand, state));
    }
    public static void openPort(ServerPlayer player, BlockPos pos) {
        if (!near(player, pos)) return;
        if (player.serverLevel().getBlockEntity(pos) instanceof TransferPortBlockEntity port)
            showPort(player, pos, port, "", "");
    }
    private static boolean near(ServerPlayer player, BlockPos pos) {
        return player.serverLevel().hasChunkAt(pos) &&
                player.distanceToSqr(pos.getX()+.5D, pos.getY()+.5D, pos.getZ()+.5D) <= 64;
    }
    private static void showBuffer(ServerPlayer player, BlockPos pos, int hand, FrequencyId id,
                                   BufferTier physicalTier, String warning, boolean confirmed, String pending) {
        CompoundTag state = new CompoundTag();
        state.putInt("Tier", physicalTier.ordinal()+1);
        state.putString("Warning", warning);
        state.putBoolean("Confirm", confirmed);
        state.putString("Pending", pending);
        state.putBoolean("Assigned", id != null);
        if (id != null) {
            state.putString("Frequency", id.displayNumber());
            state.putBoolean("Private", id.scope() == FrequencyId.Scope.PRIVATE);
            if (!id.accessibleBy(player.getUUID())) {
                state.putString("Error", "Private frequency: access denied");
            } else {
                var frequency = FrequencySavedData.get(player.serverLevel()).activateBuffer(id, physicalTier);
                if (frequency.tier().ordinal() > physicalTier.ordinal()) {
                    state.putString("Error", "This Buffer cannot display the contents of " + id.displayNumber() + " due to tier incompatibility.");
                } else {
                    state.putInt("EffectiveTier", frequency.tier().ordinal()+1);
                    state.putInt("Slots", frequency.tier().slots());
                    state.putInt("Occupied", frequency.occupiedSlots());
                    state.putLong("TotalItems", frequency.totalItems());
                    state.putString("Sort", frequency.sorting().name());
                    ListTag items = new ListTag();
                    for (var entry : frequency.sortedSnapshot()) {
                        CompoundTag t = new CompoundTag();
                        t.putLong("EntryId", entry.id());
                        t.putString("Name", entry.prototype().getHoverName().getString());
                        t.putLong("Count", entry.quantity());
                        items.add(t);
                    }
                    state.put("Items", items);
                    // A small, static player-inventory snapshot is sent only when opening
                    // or updating this GUI, never during the Port's transfer tick.
                    ListTag personal = new ListTag();
                    for (int i = 0; i < 36; i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        CompoundTag item = new CompoundTag();
                        item.putString("Name", stack.isEmpty() ? "(empty)" : stack.getHoverName().getString());
                        item.putInt("Count", stack.getCount());
                        personal.add(item);
                    }
                    state.put("PlayerItems", personal);
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new OpenConfig("buffer", pos, hand, state));
    }
    private static void showPort(ServerPlayer player, BlockPos pos, TransferPortBlockEntity port, String message, String pending) {
        CompoundTag state = new CompoundTag();
        state.putString("Warning", message);
        state.putString("Pending", pending);
        FrequencyId id = port.binding();
        state.putBoolean("Assigned", id != null);
        if (id != null) { state.putString("Frequency", id.displayNumber()); state.putBoolean("Private", id.scope() == FrequencyId.Scope.PRIVATE); }
        state.putBoolean("CanConfigure", port.canConfigure(player.getUUID()));
        state.putBoolean("Stack", port.stackMode());
        state.putInt("Slots", port.filterCapacity());
        int[] modes = new int[6];
        for (Direction face : Direction.values()) modes[face.ordinal()] = port.faceMode(face).ordinal();
        state.putIntArray("Faces", modes);
        state.put("InputFilter", port.inputFilter().save());
        state.put("OutputFilter", port.outputFilter().save());
        PacketDistributor.sendToPlayer(player, new OpenConfig("port", pos, -1, state));
    }

    private static void handleAction(Action request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (request.hand >= 0) {
            if (request.hand > 1) return;
            InteractionHand hand = request.hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof TransferBufferItem item) {
                changeBuffer(player, request, TransferBufferItem.binding(held), item.tier(), held, null);
            } else if (held.getItem() instanceof TagCardItem && request.action.equals("tag_set")) {
                String warning = TagCardItem.setTag(held, request.value) ? "" : "Invalid item tag";
                showTag(player, request.hand, TagCardItem.tag(held), warning);
            }
        } else {
            if (!near(player, request.pos)) return;
            var be = player.serverLevel().getBlockEntity(request.pos);
            if (be instanceof TransferBufferBlockEntity buffer) {
                changeBuffer(player, request, buffer.binding(),
                        ((TransferBufferBlock) buffer.getBlockState().getBlock()).tier(), null, buffer);
            } else if (be instanceof TransferPortBlockEntity port) {
                changePort(player, request, port);
            }
        }
    }
    private static FrequencyId selected(ServerPlayer player, String spec) {
        // Spec deliberately contains no owner UUID; private owner derives from authenticated player.
        String[] pieces = spec.split(":", -1);
        if (pieces.length != 2 || !pieces[0].matches("[0-9]{3}")) return null;
        int number = Integer.parseInt(pieces[0]);
        return switch (pieces[1]) {
            case "private" -> FrequencyId.privateFrequency(number, player.getUUID());
            case "public" -> FrequencyId.publicFrequency(number);
            default -> null;
        };
    }
    private static void changeBuffer(ServerPlayer player, Action req, FrequencyId current, BufferTier tier,
                                     ItemStack held, TransferBufferBlockEntity block) {
        String message = "";
        boolean confirm = false;
        FrequencySavedData data = FrequencySavedData.get(player.serverLevel());
        if (req.action.equals("bind") || req.action.equals("confirm")) {
            FrequencyId candidate = selected(player, req.value);
            if (current != null) message = "Frequency assignment is permanent.";
            else if (candidate == null) message = "Enter a three-digit frequency and select Public or Private.";
            else if (data.exists(candidate) && req.action.equals("bind")) {
                message = "Frequency already exists. Joining it permanently will share its inventory.";
                confirm = true;
            } else {
                if (held != null) { TransferBufferItem.setBinding(held, candidate); data.activateBuffer(candidate, tier); }
                else if (block != null) block.bind(candidate, tier);
                current = candidate;
            }
        } else if ((req.action.equals("deposit") || req.action.equals("withdraw"))
                && current != null && current.accessibleBy(player.getUUID())) {
            var frequency = data.find(current).orElse(null);
            if (frequency == null || frequency.tier().ordinal() > tier.ordinal()) {
                message = "This Buffer cannot access a higher-tier frequency.";
            } else if (req.action.equals("deposit")) {
                int maximum = "stack".equals(req.value) ? 64 : 1;
                int inserted = BufferInventoryActions.deposit(player, frequency, req.slot, maximum);
                if (inserted > 0) data.changed();
                else message = "Nothing deposited (inventory empty or Buffer full).";
            } else {
                long entryId;
                try { entryId = Long.parseLong(req.value); }
                catch (NumberFormatException ex) { entryId = -1L; }
                int maximum = req.slot == 64 ? 64 : req.slot == 1 ? 1 : 0;
                int delivered = BufferInventoryActions.withdraw(player, frequency, entryId, maximum);
                if (delivered > 0) data.changed();
                else message = "Nothing withdrawn (item missing or inventory full).";
            }
        } else if (req.action.equals("sort") && current != null && current.accessibleBy(player.getUUID())) {
            var frequency = data.find(current).orElse(null);
            if (frequency != null && frequency.tier().ordinal() <= tier.ordinal()) {
                try {
                    frequency.sorting(NetworkInventory.SortMode.valueOf(req.value));
                    data.changed();
                } catch (IllegalArgumentException ex) { message = "Invalid sorting mode"; }
            }
        }
        showBuffer(player, req.pos, req.hand, current, tier, message, confirm, confirm ? req.value : "");
    }
    private static void changePort(ServerPlayer player, Action req, TransferPortBlockEntity port) {
        UUID actor = player.getUUID();
        String message = "";
        try {
            switch (req.action) {
                case "bind" -> {
                    FrequencyId id = selected(player, req.value);
                    if (id == null || !FrequencySavedData.get(player.serverLevel()).exists(id) || !port.assign(id, actor))
                        message = "Frequency does not exist, or the Port is already assigned.";
                }
                case "face" -> {
                    Direction dir = Direction.values()[req.slot];
                    TransferPortBlockEntity.FaceMode mode = TransferPortBlockEntity.FaceMode.valueOf(req.value);
                    if (!port.setMode(dir, mode, actor)) message = "Input/output overlap. Configure disjoint filters first.";
                }
                case "stack" -> { if (!port.setStackMode(req.value.equals("true"), actor)) message = "Access denied"; }
                case "rule" -> {
                    String[] parts = req.value.split("\\|", 2);
                    if (parts.length != 2 || !port.setFilterRule(parts[0].equals("input"), req.slot, parts[1], actor))
                        message = "Invalid item/tag or input/output filter overlap.";
                }
                case "apply_card" -> {
                    ItemStack card = player.getOffhandItem();
                    if (!(card.getItem() instanceof TagCardItem)) card = player.getMainHandItem();
                    String tag = TagCardItem.tag(card);
                    if (tag.isEmpty() || !port.setFilterRule(req.value.equals("input"), req.slot, "#"+tag, actor))
                        message = "Hold a configured Tag Card (offhand recommended), or check filter overlap.";
                }
                case "filter_mode" -> {
                    String[] parts = req.value.split(":", 2);
                    if (parts.length != 2 || !port.setFilterWhitelist(parts[0].equals("input"), parts[1].equals("white"), actor))
                        message = "Cannot permit the same item in both directions.";
                }
                default -> message = "Unknown configuration action";
            }
        } catch (RuntimeException invalid) { message = "Invalid configuration value"; }
        showPort(player, req.pos, port, message, req.action.equals("bind") ? req.value : "");
    }
}
