package dev.frequencyworks.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.frequencyworks.block.TransferBufferBlock;
import dev.frequencyworks.blockentity.TransferBufferBlockEntity;
import dev.frequencyworks.blockentity.TransferPortBlockEntity;
import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.item.TransferBufferItem;
import dev.frequencyworks.world.FrequencySavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Temporary alpha-only controls while server-validated GUIs are built.
 * Clients never supply a UUID: private frequency owner always comes from server player. */
public final class FrequencyCommands {
    private FrequencyCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("fw")
                .then(Commands.literal("buffer")
                    .then(Commands.argument("number", IntegerArgumentType.integer(0, 999))
                        .then(Commands.literal("public").executes(c -> bindBuffer(c.getSource(), IntegerArgumentType.getInteger(c, "number"), false, false))
                            .then(Commands.literal("confirm").executes(c -> bindBuffer(c.getSource(), IntegerArgumentType.getInteger(c, "number"), false, true))))
                        .then(Commands.literal("private").executes(c -> bindBuffer(c.getSource(), IntegerArgumentType.getInteger(c, "number"), true, false))
                            .then(Commands.literal("confirm").executes(c -> bindBuffer(c.getSource(), IntegerArgumentType.getInteger(c, "number"), true, true))))))
                .then(Commands.literal("port")
                    .then(Commands.argument("number", IntegerArgumentType.integer(0, 999))
                        .then(Commands.literal("public").executes(c -> bindPort(c.getSource(), IntegerArgumentType.getInteger(c, "number"), false)))
                        .then(Commands.literal("private").executes(c -> bindPort(c.getSource(), IntegerArgumentType.getInteger(c, "number"), true)))))
                .then(Commands.literal("face")
                    .then(Commands.argument("side", StringArgumentType.word())
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .executes(c -> face(c.getSource(), StringArgumentType.getString(c, "side"), StringArgumentType.getString(c, "mode"))))))
                .then(Commands.literal("mode")
                    .then(Commands.literal("single").executes(c -> amountMode(c.getSource(), false)))
                    .then(Commands.literal("stack").executes(c -> amountMode(c.getSource(), true))))
                .then(Commands.literal("sort")
                    .then(Commands.argument("order", StringArgumentType.word())
                        .executes(c -> sort(c.getSource(), StringArgumentType.getString(c, "order")))))
                .then(Commands.literal("info").executes(c -> info(c.getSource()))));
    }

    private static FrequencyId key(ServerPlayer player, int number, boolean isPrivate) {
        return isPrivate ? FrequencyId.privateFrequency(number, player.getUUID()) : FrequencyId.publicFrequency(number);
    }
    private static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
    private static int error(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
    private static BlockEntity lookingAt(ServerPlayer player) {
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = block.getBlockPos();
        return player.serverLevel().getBlockEntity(pos);
    }

    private static int bindBuffer(CommandSourceStack source, int number, boolean priv, boolean confirm) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        FrequencyId id = key(player, number, priv);
        FrequencySavedData store = FrequencySavedData.get(player.serverLevel());
        BlockEntity target = lookingAt(player);
        ItemStack held = player.getMainHandItem();
        boolean isHeldBuffer = held.getItem() instanceof TransferBufferItem;
        if (!(target instanceof TransferBufferBlockEntity) && !isHeldBuffer)
            return error(source, "Look at a Transfer Buffer or hold one in your main hand.");
        if (store.exists(id) && !confirm)
            return error(source, "Frequency " + id.displayNumber() + " already exists. Repeat with 'confirm' at the end to join it permanently.");
        if (target instanceof TransferBufferBlockEntity buffer) {
            BufferTier tier = ((TransferBufferBlock) buffer.getBlockState().getBlock()).tier();
            if (!buffer.bind(id, tier)) return error(source, "This Buffer is already assigned and cannot be reassigned.");
        } else {
            if (TransferBufferItem.binding(held) != null) return error(source, "This Buffer is already assigned and cannot be reassigned.");
            TransferBufferItem.setBinding(held, id);
            store.activateBuffer(id, ((TransferBufferItem) held.getItem()).tier());
        }
        say(source, "Permanently bound Buffer to " + id.scope() + " frequency " + id.displayNumber());
        return 1;
    }

    private static TransferPortBlockEntity portTarget(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockEntity target = lookingAt(source.getPlayerOrException());
        return target instanceof TransferPortBlockEntity port ? port : null;
    }
    private static int bindPort(CommandSourceStack source, int number, boolean priv) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var port = portTarget(source);
        if (port == null) return error(source, "Look directly at a placed Transfer Port.");
        var id = key(player, number, priv);
        if (!FrequencySavedData.get(player.serverLevel()).exists(id))
            return error(source, "That frequency does not exist. Create it with a Transfer Buffer first.");
        if (!port.assign(id, player.getUUID())) return error(source, "Port already bound or unauthorized.");
        say(source, "Port connected to " + id.scope() + " frequency " + id.displayNumber());
        return 1;
    }
    private static int face(CommandSourceStack source, String sideText, String modeText) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var port = portTarget(source);
        if (port == null) return error(source, "Look directly at a placed Transfer Port.");
        try {
            Direction side = Direction.valueOf(sideText.toUpperCase(java.util.Locale.ROOT));
            TransferPortBlockEntity.FaceMode mode = TransferPortBlockEntity.FaceMode.valueOf(modeText.toUpperCase(java.util.Locale.ROOT));
            if (!port.setMode(side, mode, source.getPlayerOrException().getUUID()))
                return error(source, "Port not assigned, not yours, or BOTH mode unavailable until filters are implemented.");
            say(source, side + " machine face is now " + mode + " (input=machine->frequency; output=frequency->machine)");
            return 1;
        } catch (IllegalArgumentException ex) { return error(source, "Use face north/south/east/west/up/down and mode none/input/output."); }
    }
    private static int amountMode(CommandSourceStack source, boolean stack) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var port = portTarget(source);
        if (port == null || !port.setStackMode(stack, source.getPlayerOrException().getUUID()))
            return error(source, "Look directly at your configured Transfer Port.");
        say(source, "Port now using " + (stack ? "full-stack" : "individual item") + " transfer rate.");
        return 1;
    }
    private static int sort(CommandSourceStack source, String text) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        FrequencyId id = null;
        BlockEntity target = lookingAt(player);
        if (target instanceof TransferBufferBlockEntity buffer) id = buffer.binding();
        if (id == null) id = TransferBufferItem.binding(player.getMainHandItem());
        if (id == null || !id.accessibleBy(player.getUUID())) return error(source, "Look at or hold your assigned Transfer Buffer.");
        var store = FrequencySavedData.get(player.serverLevel());
        var network = store.find(id).orElse(null);
        if (network == null) return error(source, "Frequency not found.");
        BufferTier viewingTier = null;
        if (target instanceof TransferBufferBlockEntity buffer) {
            viewingTier = ((TransferBufferBlock) buffer.getBlockState().getBlock()).tier();
        } else if (player.getMainHandItem().getItem() instanceof TransferBufferItem item) {
            viewingTier = item.tier();
        }
        if (viewingTier == null || viewingTier.ordinal() < network.tier().ordinal())
            return error(source, "This Buffer cannot configure a higher-tier frequency.");
        try {
            var order = dev.frequencyworks.core.NetworkInventory.SortMode.valueOf(text.toUpperCase(java.util.Locale.ROOT));
            network.sorting(order);
            store.changed();
            say(source, "Frequency " + id.displayNumber() + " sorting: " + order);
            return 1;
        } catch (IllegalArgumentException ex) { return error(source, "Sorting: quantity_asc, quantity_desc, alphabetical, mod_alphabetical"); }
    }
    private static int info(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockEntity be = lookingAt(player);
        if (be instanceof TransferPortBlockEntity port) { say(source, port.status()); return 1; }
        FrequencyId id = be instanceof TransferBufferBlockEntity buffer ? buffer.binding() : TransferBufferItem.binding(player.getMainHandItem());
        if (id == null) return error(source, "Look at a Port/Buffer or hold a bound Buffer.");
        if (!id.accessibleBy(player.getUUID())) return error(source, "Private frequency: access denied.");
        var network = FrequencySavedData.get(player.serverLevel()).find(id).orElse(null);
        if (network == null) return error(source, "Unknown frequency.");
        say(source, "Frequency " + id.displayNumber() + " " + id.scope() + " tier " + (network.tier().ordinal()+1)
                + ", " + network.totalItems() + " stored items, " + network.occupiedSlots() + "/" + network.tier().slots()
                + " slots, sorting " + network.sorting());
        return 1;
    }
}
