package dev.frequencyworks.client;

import dev.frequencyworks.network.ConfigNetworking;
import net.minecraft.client.Minecraft;

/** Called only when a registered clientbound config packet arrives. */
public final class ConfigClient {
    private ConfigClient() {}
    public static void open(ConfigNetworking.OpenConfig packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof ConfigScreen existing && existing.sameTarget(packet)) {
            client.setScreen(new ConfigScreen(packet, existing.page(), existing.bank(), existing.selectedSlot(), existing.depositSlot()));
        } else {
            client.setScreen(new ConfigScreen(packet));
        }
    }
}
