package com.spear_boost;

import com.spear_boost.mixin.MinecraftClientInvoker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
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

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.interactionManager == null) return;

        if (!KeyBinds.boostKey.isPressed()) {
            reset();
            return;
        }

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

            // best var - empty slot
            if (stack.isEmpty()) {
                return i;
            }

            // if can use
            boolean isFood = stack.get(DataComponentTypes.FOOD) != null;
            boolean hasUseAction = stack.getUseAction() != UseAction.NONE;

            if (isFood || hasUseAction) continue;

            // deny weapon
            Identifier id = Registries.ITEM.getId(stack.getItem());
            String path = id.getPath();

            if (
                    path.endsWith("_sword") ||
                            path.endsWith("_axe") ||
                            path.endsWith("_pickaxe") ||
                            path.endsWith("_shovel") ||
                            path.endsWith("_hoe")
            ) {
                continue;
            }

            // deny trident/spear
            if (path.endsWith("_spear") || path.equals("trident")) {
                continue;
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