package dev.frequencyworks;

import dev.frequencyworks.command.FrequencyCommands;
import dev.frequencyworks.network.ConfigNetworking;
import dev.frequencyworks.registration.ModContent;
import dev.frequencyworks.security.BufferSecurityEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FrequencyWorks.MOD_ID)
public final class FrequencyWorks {
    public static final String MOD_ID = "frequencyworks";
    public FrequencyWorks(IEventBus modBus) {
        ModContent.BLOCKS.register(modBus);
        ModContent.ITEMS.register(modBus);
        ModContent.BLOCK_ENTITIES.register(modBus);
        ModContent.MENUS.register(modBus);
        modBus.addListener(ConfigNetworking::register);
        NeoForge.EVENT_BUS.addListener(FrequencyCommands::register);
        NeoForge.EVENT_BUS.addListener(BufferSecurityEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(BufferSecurityEvents::onExplosion);
        NeoForge.EVENT_BUS.addListener(BufferSecurityEvents::onGroundPickup);
        NeoForge.EVENT_BUS.addListener(BufferSecurityEvents::onSlotStack);
    }
}
