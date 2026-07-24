package com.spear_boost;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {

    private static final int DEFAULT_BOOST_INTERVAL = 5;
    private static final int DEFAULT_DELAY_BEFORE_HIT = 0;

    private BoostIntervalSlider boostSlider;
    private DelayBeforeHitSlider delaySlider;
    private Button resetBoostButton;
    private Button resetDelayButton;

    private int authorX, authorY, authorWidth, authorHeight;

    protected ConfigScreen() {
        super(Component.literal("key.spear_boost.open_config"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = height / 2 - 55;

        boostSlider = new BoostIntervalSlider(centerX - 100, startY, 200, 20);
        addRenderableWidget(boostSlider);

        resetBoostButton = Button.builder(
                Component.literal("↺"),
                button -> {
                    Config.boostInterval = DEFAULT_BOOST_INTERVAL;
                    boostSlider.updateFromConfig();
                    updateResetButtons();
                }
        ).bounds(centerX + 105, startY, 20, 20).build();
        addRenderableWidget(resetBoostButton);

        delaySlider = new DelayBeforeHitSlider(centerX - 100, startY + 35, 200, 20);
        addRenderableWidget(delaySlider);

        resetDelayButton = Button.builder(
                Component.literal("↺"),
                button -> {
                    Config.delayBeforeHit = DEFAULT_DELAY_BEFORE_HIT;
                    delaySlider.updateFromConfig();
                    updateResetButtons();
                }
        ).bounds(centerX + 105, startY + 35, 20, 20).build();
        addRenderableWidget(resetDelayButton);

        addRenderableWidget(Button.builder(
                Component.translatable("screen.spear_boost.close"),
                button -> this.minecraft.setScreen(null)
        ).bounds(centerX - 100, startY + 85, 200, 20).build());

        updateResetButtons();
    }

    private void updateResetButtons() {
        resetBoostButton.active = (Config.boostInterval != DEFAULT_BOOST_INTERVAL);
        resetDelayButton.active = (Config.delayBeforeHit != DEFAULT_DELAY_BEFORE_HIT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int centerX = width / 2;
        int startY = height / 2 - 55;

        // subtitle
        graphics.text(
                font,
                Component.translatable("screen.spear_boost.subtitle"),
                width / 2 - font.width(Component.translatable("screen.spear_boost.subtitle")) / 2,
                height / 2 - 75,
                0xAAAAAA,
                true
        );

        // warning
        graphics.text(
                font,
                Component.translatable("warning.spear_boost.low"),
                centerX - 100,
                startY - 12,
                0xFFFF5555,
                true
        );

        graphics.text(
                font,
                Component.translatable("warning.spear_boost.lag"),
                centerX - 100,
                startY + 23,
                0xFFFFFF00,
                true
        );

        // author
        Component authorText = Component.translatable("screen.spear_boost.author");
        authorX = centerX - 100;
        authorY = startY + 60;
        authorWidth = font.width(authorText);
        authorHeight = font.lineHeight;

        graphics.text(
                font,
                authorText,
                authorX,
                authorY,
                0xFF888888,
                true
        );
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {

        double mouseX = event.x();
        double mouseY = event.y();

        if (mouseX >= authorX && mouseX <= authorX + authorWidth &&
                mouseY >= authorY && mouseY <= authorY + authorHeight) {

            net.minecraft.util.Util.getPlatform().openUri("https://github.com/PortalGum");
            return true;
        }

        return super.mouseClicked(event, doubled);
    }



    // sliders

    private class BoostIntervalSlider extends AbstractSliderButton {
        public BoostIntervalSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0.0);
            updateFromConfig();
        }
        public void updateFromConfig() {
            this.value = Config.boostInterval / 20.0;
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("slider.spear_boost.interval", Config.boostInterval));
        }
        @Override
        protected void applyValue() {
            Config.boostInterval = (int) Math.round(this.value * 20);
            updateMessage();
            updateResetButtons();
        }
    }

    private class DelayBeforeHitSlider extends AbstractSliderButton {
        public DelayBeforeHitSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0.0);
            updateFromConfig();
        }
        public void updateFromConfig() {
            this.value = Config.delayBeforeHit / 20.0;
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("slider.spear_boost.delay", Config.delayBeforeHit));
        }
        @Override
        protected void applyValue() {
            Config.delayBeforeHit = (int) Math.round(this.value * 20);
            updateMessage();
            updateResetButtons();
        }
    }
}