package com.spear_boost;

import com.spear_boost.mixin.MinecraftClientInvoker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.interactionManager == null) return;

        if (!KeyBinds.boostKey.isPressed()) {
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

        PlayerInventory inv = client.player.getInventory();

        // update
        cachedSpearSlot = findspear(inv);
        if (cachedSpearSlot == -1) {
            client.player.sendMessage(Text.translatable("error.spear_boost.nolunge"), true);
            reset();
            return;
        }

        cachedSafeSlot = findSafeSlot(inv, cachedSpearSlot);
        if (cachedSafeSlot == -1) {
            client.player.sendMessage(Text.translatable("error.spear_boost.noslots"), true);
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

                client.getNetworkHandler().sendPacket(
                        new UpdateSelectedSlotC2SPacket(inv.getSelectedSlot())
                );

                phaseTimer = Config.boostInterval;
                phase = Phase.SPEAR_SLOT;
            }

            case SPEAR_SLOT -> {
                inv.setSelectedSlot(cachedSpearSlot);

                client.getNetworkHandler().sendPacket(
                        new UpdateSelectedSlotC2SPacket(inv.getSelectedSlot())
                );

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

    private static int findspear(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            Identifier itemId = Registries.ITEM.getId(stack.getItem());

            // spears
            if (!itemId.getPath().endsWith("_spear")) continue;

            // check enchant lunge
            var enchants = stack.getEnchantments().getEnchantments();

            for (var entry : enchants) {
                Identifier enchId = entry.getKey().get().getValue();
                if (enchId.getPath().equals("lunge")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findSafeSlot(PlayerInventory inv, int spearSlot) {
        int fallback = -1;

        for (int i = 0; i < 9; i++) {
            if (i == spearSlot) continue;

            ItemStack stack = inv.getStack(i);

            if (stack.isEmpty()) return i;

            if (stack.get(DataComponentTypes.FOOD) != null) continue;
            if (stack.getUseAction() != UseAction.NONE) continue;

            // check attribute
            var attributeModifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

            if (attributeModifiers != null) {
                boolean hasAttackSpeed = attributeModifiers.modifiers().stream()
                        .anyMatch(entry -> entry.attribute().equals(EntityAttributes.ATTACK_SPEED));

                if (hasAttackSpeed) {
                    continue;
                }
            }

            fallback = i;
        }

        return fallback;
    }

    private static void sendAttackPacket(MinecraftClient client) {
        if (client == null) return;

        ((MinecraftClientInvoker) client).invokeDoAttack();
    }
}