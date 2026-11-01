package dev.frequencyworks.registration;

import dev.frequencyworks.FrequencyWorks;
import dev.frequencyworks.block.TransferBufferBlock;
import dev.frequencyworks.block.TransferPortBlock;
import dev.frequencyworks.blockentity.TransferBufferBlockEntity;
import dev.frequencyworks.blockentity.TransferPortBlockEntity;
import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.item.TransferBufferItem;
import dev.frequencyworks.item.TagCardItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    private ModContent() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrequencyWorks.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrequencyWorks.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FrequencyWorks.MOD_ID);

    public static final DeferredBlock<TransferBufferBlock> BUFFER_1 = buffer("transfer_buffer_t1", BufferTier.TETHERED);
    public static final DeferredBlock<TransferBufferBlock> BUFFER_2 = buffer("transfer_buffer_t2", BufferTier.GILDED);
    public static final DeferredBlock<TransferBufferBlock> BUFFER_3 = buffer("transfer_buffer_t3", BufferTier.BLAZING);
    public static final DeferredBlock<TransferBufferBlock> BUFFER_4 = buffer("transfer_buffer_t4", BufferTier.ENDWOVEN);
    public static final DeferredBlock<TransferBufferBlock> BUFFER_5 = buffer("transfer_buffer_t5", BufferTier.DRAGONBOUND);

    public static final DeferredBlock<TransferPortBlock> PORT_1 = port("transfer_port_t1", 0);
    public static final DeferredBlock<TransferPortBlock> PORT_2 = port("transfer_port_t2", 1);
    public static final DeferredBlock<TransferPortBlock> PORT_3 = port("transfer_port_t3", 2);
    public static final DeferredBlock<TransferPortBlock> PORT_4 = port("transfer_port_t4", 3);
    public static final DeferredBlock<TransferPortBlock> PORT_5 = port("transfer_port_t5", 4);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransferBufferBlockEntity>> BUFFER_ENTITY =
            BLOCK_ENTITIES.register("transfer_buffer", () -> BlockEntityType.Builder.of(
                    TransferBufferBlockEntity::new,
                    BUFFER_1.get(), BUFFER_2.get(), BUFFER_3.get(), BUFFER_4.get(), BUFFER_5.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransferPortBlockEntity>> PORT_ENTITY =
            BLOCK_ENTITIES.register("transfer_port", () -> BlockEntityType.Builder.of(
                    TransferPortBlockEntity::new,
                    PORT_1.get(), PORT_2.get(), PORT_3.get(), PORT_4.get(), PORT_5.get()
            ).build(null));

    public static final DeferredBlock<Block> ITEM_SEQUENCER = simpleBlock("item_sequencer");
    public static final DeferredBlock<Block> FREQUENCY_MONITOR = simpleBlock("frequency_monitor");

    public static final DeferredItem<TransferBufferItem> BUFFER_ITEM_1 = bufferItem("transfer_buffer_t1", BUFFER_1, BufferTier.TETHERED);
    public static final DeferredItem<TransferBufferItem> BUFFER_ITEM_2 = bufferItem("transfer_buffer_t2", BUFFER_2, BufferTier.GILDED);
    public static final DeferredItem<TransferBufferItem> BUFFER_ITEM_3 = bufferItem("transfer_buffer_t3", BUFFER_3, BufferTier.BLAZING);
    public static final DeferredItem<TransferBufferItem> BUFFER_ITEM_4 = bufferItem("transfer_buffer_t4", BUFFER_4, BufferTier.ENDWOVEN);
    public static final DeferredItem<TransferBufferItem> BUFFER_ITEM_5 = bufferItem("transfer_buffer_t5", BUFFER_5, BufferTier.DRAGONBOUND);

    // Vanilla-clean item stacks. There is intentionally NO custom data component on Ports.
    public static final DeferredItem<BlockItem> PORT_ITEM_1 = ITEMS.registerSimpleBlockItem(PORT_1);
    public static final DeferredItem<BlockItem> PORT_ITEM_2 = ITEMS.registerSimpleBlockItem(PORT_2);
    public static final DeferredItem<BlockItem> PORT_ITEM_3 = ITEMS.registerSimpleBlockItem(PORT_3);
    public static final DeferredItem<BlockItem> PORT_ITEM_4 = ITEMS.registerSimpleBlockItem(PORT_4);
    public static final DeferredItem<BlockItem> PORT_ITEM_5 = ITEMS.registerSimpleBlockItem(PORT_5);
    public static final DeferredItem<BlockItem> SEQUENCER_ITEM = ITEMS.registerSimpleBlockItem(ITEM_SEQUENCER);
    public static final DeferredItem<BlockItem> MONITOR_ITEM = ITEMS.registerSimpleBlockItem(FREQUENCY_MONITOR);
    public static final DeferredItem<Item> SEQUENCE_TABLET = ITEMS.registerSimpleItem("sequence_tablet", new Item.Properties().stacksTo(1));
    public static final DeferredItem<TagCardItem> TAG_CARD = ITEMS.register("tag_card", () -> new TagCardItem(new Item.Properties()));
    public static final DeferredItem<Item> INTERACTION_CARD = ITEMS.registerSimpleItem("interaction_card", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> REDSTONE_CARD = ITEMS.registerSimpleItem("redstone_card", new Item.Properties().stacksTo(1));

    private static DeferredBlock<TransferBufferBlock> buffer(String name, BufferTier tier) {
        return BLOCKS.register(name, () -> new TransferBufferBlock(BlockBehaviour.Properties.of()
                .strength(2.0F, tier.ordinal() >= 2 ? 3_600_000.0F : 2.0F)
                .noLootTable(), tier));
    }
    private static DeferredBlock<TransferPortBlock> port(String name, int tier) {
        return BLOCKS.register(name, () -> new TransferPortBlock(BlockBehaviour.Properties.of().strength(1.0F), tier));
    }
    private static DeferredBlock<Block> simpleBlock(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().strength(2.0F)));
    }
    private static DeferredItem<TransferBufferItem> bufferItem(String name, Supplier<? extends Block> block, BufferTier tier) {
        return ITEMS.register(name, () -> new TransferBufferItem(block.get(), new Item.Properties(), tier));
    }
}
