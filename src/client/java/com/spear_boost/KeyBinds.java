package com.spear_boost;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBinds {

    public static KeyMapping boostKey;
    public static KeyMapping openConfigKey;

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(
                    Identifier.fromNamespaceAndPath("spear_boost", "main")
            );

    public static void register() {
        boostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spear_boost.boost",
                InputConstants.KEY_X,
                CATEGORY
        ));

        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spear_boost.open_config",
                InputConstants.KEY_I,
                CATEGORY
        ));
    }
}