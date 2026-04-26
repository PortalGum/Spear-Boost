package com.spear_boost;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;


public class ConfigScreen extends Screen {

    private static final int DEFAULT_BOOST_INTERVAL = 5;
    private static final int DEFAULT_DELAY_BEFORE_HIT = 0;

    private BoostIntervalSlider boostSlider;
    private DelayBeforeHitSlider delaySlider;

    private ButtonWidget resetBoostButton;
    private ButtonWidget resetDelayButton;

    private int authorX, authorY, authorWidth, authorHeight;


    protected ConfigScreen() {
        super(Text.literal("key.spear_boost.open_config"));
    }

    @Override
    protected void init() {

        int centerX = width / 2;
        int startY = height / 2 - 55;

        // --- BOOST INTERVAL SLIDER ---
        boostSlider = new BoostIntervalSlider(
                centerX - 100,
                startY,
                200,
                20
        );
        addDrawableChild(boostSlider);

        resetBoostButton = ButtonWidget.builder(
                Text.literal("↺"),
                button -> {
                    Config.boostInterval = DEFAULT_BOOST_INTERVAL;
                    boostSlider.updateFromConfig();
                    updateResetButtons();
                }
        ).dimensions(centerX + 105, startY, 20, 20).build();

        addDrawableChild(resetBoostButton);

        // --- DELAY SLIDER ---
        delaySlider = new DelayBeforeHitSlider(
                centerX - 100,
                startY + 35,
                200,
                20
        );
        addDrawableChild(delaySlider);

        resetDelayButton = ButtonWidget.builder(
                Text.literal("↺"),
                button -> {
                    Config.delayBeforeHit = DEFAULT_DELAY_BEFORE_HIT;
                    delaySlider.updateFromConfig();
                    updateResetButtons();
                }
        ).dimensions(centerX + 105, startY + 35, 20, 20).build();

        addDrawableChild(resetDelayButton);

        // --- CLOSE BUTTON ---
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.spear_boost.close"),
                button -> close()
        ).dimensions(centerX - 100, startY + 85, 200, 20).build());

        updateResetButtons();
    }

    private void updateResetButtons() {
        resetBoostButton.active = (Config.boostInterval != DEFAULT_BOOST_INTERVAL);
        resetDelayButton.active = (Config.delayBeforeHit != DEFAULT_DELAY_BEFORE_HIT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        this.renderInGameBackground(context);

        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int startY = height / 2 - 55;

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.spear_boost.subtitle"),
                width / 2,
                height / 2 - 75,
                0xAAAAAA
        );

        context.drawText(
                textRenderer,
                Text.translatable("warning.spear_boost.low"),
                centerX - 100,
                startY - 12,
                0xFFFF5555,
                true
        );

        context.drawText(
                textRenderer,
                Text.translatable("warning.spear_boost.lag"),
                centerX - 100,
                startY + 23,
                0xFFFFFF00,
                true
        );

        Text authorText = Text.translatable("screen.spear_boost.author");

        authorX = centerX - 100;
        authorY = startY + 60;

        authorWidth = textRenderer.getWidth(authorText);
        authorHeight = 10;

        context.drawText(
                textRenderer,
                authorText,
                authorX,
                authorY,
                0xFF888888,
                true
        );


    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {

        double mouseX = click.x();
        double mouseY = click.y();

        // check click to text
        if (mouseX >= authorX && mouseX <= authorX + authorWidth &&
                mouseY >= authorY && mouseY <= authorY + authorHeight) {

            Util.getOperatingSystem().open("https://github.com/PortalGum");
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // sliders

    private class BoostIntervalSlider extends SliderWidget {

        public BoostIntervalSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), 0.0);
            updateFromConfig();
        }

        public void updateFromConfig() {
            // 0..20
            this.value = Config.boostInterval / 20.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("slider.spear_boost.interval", Config.boostInterval));
        }

        @Override
        protected void applyValue() {
            Config.boostInterval = (int) Math.round(this.value * 20);
            updateMessage();
            updateResetButtons();
        }
    }

    private class DelayBeforeHitSlider extends SliderWidget {

        public DelayBeforeHitSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), 0.0);
            updateFromConfig();
        }

        public void updateFromConfig() {
            // 0..20
            this.value = Config.delayBeforeHit / 20.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("slider.spear_boost.delay", Config.delayBeforeHit));
        }

        @Override
        protected void applyValue() {
            Config.delayBeforeHit = (int) Math.round(this.value * 20);
            updateMessage();
            updateResetButtons();
        }
    }
}