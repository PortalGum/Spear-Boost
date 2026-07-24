package com.spear_boost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class SpearBoostClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        KeyBinds.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null) return;

            // boost
            BoostLogic.tick(client);

            // open settings
            while (KeyBinds.openConfigKey.consumeClick()) {
                client.setScreen(new ConfigScreen());
            }
        });
    }
}