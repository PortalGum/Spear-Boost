package com.spear_boost;

import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class BoostLogic {

    private enum Phase {
        SAFE_SLOT,
        SPEAR_SLOT,
        HIT
    }

    private static Phase phase = Phase.SAFE_SLOT;
    private static int phaseTimer = 0;
    private static int cachedSafeSlot = -1;
    private static int cachedSpearSlot = -1;
    private static boolean wasKeyDown = false;

    public static void tick(Minecraft client) {
        if (client.player == null) return;
        if (client.gameMode == null) return;

        if (!KeyBinds.boostKey.isDown()) {
            reset();
            wasKeyDown = false;
            return;
        }

        boolean justPressed = !wasKeyDown;
        wasKeyDown = true;

        if (phaseTimer > 0) {
            phaseTimer--;
            return;
        }

        Inventory inv = client.player.getInventory();

        cachedSpearSlot = findSpear(inv);
        if (cachedSpearSlot == -1) {
            if (client.gui != null && client.gui.hud != null) {
                client.gui.hud.setOverlayMessage(Component.translatable("error.spear_boost.nolunge"), true);
            }
            reset();
            return;
        }

        cachedSafeSlot = findSafeSlot(inv, cachedSpearSlot);
        if (cachedSafeSlot == -1) {
            if (client.gui != null && client.gui.hud != null) {
                client.gui.hud.setOverlayMessage(Component.translatable("error.spear_boost.noslots"), true);
            }
            reset();
            return;
        }

        if (justPressed && phase == Phase.SAFE_SLOT) {
            // skip the safe-slot step on the very first tick after the key
            // was pressed so the spear hit happens right away instead of
            // waiting a full boostInterval on an idle slot switch
            phase = Phase.SPEAR_SLOT;
        }

        switch (phase) {
            case SAFE_SLOT -> {
                inv.setSelectedSlot(cachedSafeSlot);
                client.getConnection().send(new ServerboundSetCarriedItemPacket(inv.getSelectedSlot()));
                phaseTimer = Config.boostInterval;
                phase = Phase.SPEAR_SLOT;
            }
            case SPEAR_SLOT -> {
                inv.setSelectedSlot(cachedSpearSlot);
                client.getConnection().send(new ServerboundSetCarriedItemPacket(inv.getSelectedSlot()));
                if (Config.delayBeforeHit <= 0) {
                    sendAttackPacket(client);
                    phase = Phase.SAFE_SLOT;
                } else {
                    phaseTimer = Config.delayBeforeHit;
                    phase = Phase.HIT;
                }
            }
            case HIT -> {
                sendAttackPacket(client);
                phase = Phase.SAFE_SLOT;
            }
        }
    }

    private static void reset() {
        phase = Phase.SAFE_SLOT;
        phaseTimer = 0;
        cachedSafeSlot = -1;
        cachedSpearSlot = -1;
    }

    private static int findSpear(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!itemId.getPath().endsWith("_spear")) continue;

            var enchantments = stack.getEnchantments();
            for (var entry : enchantments.entrySet()) {
                Identifier enchId = entry.getKey().unwrapKey().get().identifier();
                if (enchId.getPath().equals("lunge")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findSafeSlot(Inventory inv, int spearSlot) {
        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            if (i == spearSlot) continue;
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) return i;

            if (stack.getComponents().has(DataComponents.FOOD)) continue;
            if (stack.getUseAnimation() != ItemUseAnimation.NONE) continue;

            // check attribute
            var attributeModifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (attributeModifiers != null) {
                boolean hasAttackSpeed = attributeModifiers.modifiers().stream()
                        .anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_SPEED));

                if (hasAttackSpeed) {
                    continue;
                }
            }

            fallback = i;
        }
        return fallback;
    }


    private static void sendAttackPacket(Minecraft client) {
        if (client == null) return;
        try {
            Class<?> invokerClass = Class.forName("com.spear_boost.mixin.MinecraftClientInvoker");
            invokerClass.getMethod("invokeStartAttack").invoke(invokerClass.cast(client));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}