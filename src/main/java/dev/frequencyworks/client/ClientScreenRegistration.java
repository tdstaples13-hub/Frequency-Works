package dev.frequencyworks.client;

import dev.frequencyworks.registration.ModContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import dev.frequencyworks.FrequencyWorks;

@EventBusSubscriber(modid=FrequencyWorks.MOD_ID, bus=EventBusSubscriber.Bus.MOD, value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientScreenRegistration {
    private ClientScreenRegistration() {}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(ModContent.BUFFER_MENU.get(), BufferScreen::new);
    }
}
