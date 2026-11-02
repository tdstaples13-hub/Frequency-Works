package dev.frequencyworks.client;

import dev.frequencyworks.menu.TabletMenu;
import dev.frequencyworks.network.SequencerNetworking;
import net.minecraft.client.Minecraft;

/** The Tablet GUI receives the authoritative program, not a guessed cursor item. */
public final class SequencerClient {
    private SequencerClient(){}
    public static void receive(SequencerNetworking.TabletState packet){
        Minecraft minecraft=Minecraft.getInstance();
        minecraft.execute(()->{
            if(minecraft.player!=null && minecraft.player.containerMenu instanceof TabletMenu menu
                    && menu.containerId==packet.containerId()){
                menu.setState(packet.program());
                if(minecraft.screen instanceof TabletScreen screen)screen.refreshState();
            }
        });
    }
}