package me.randemlyy.postbox.client;

import me.randemlyy.postbox.LetterContents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LetterReadScreen extends Screen {

    private static final int W = 320;
    private static final int H = 220;

    private final LetterContents contents;

    public LetterReadScreen(LetterContents contents) {
        super(Component.translatable("postbox.screen.letter"));
        this.contents = contents;
    }

    @Override
    protected void init() {
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + (W - 90) / 2, top + H - 26, 90, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;

        graphics.fill(left, top, left + W, top + H, 0xF0181818);
        graphics.outline(left, top, W, H, 0xFF808080);

        graphics.text(this.font, Component.translatable("postbox.screen.letter"), left + 10, top + 10, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("postbox.letter.read_to", this.contents.to()),
                left + 10, top + 28, 0xFFB0B0B0);
        graphics.text(this.font, Component.translatable("postbox.letter.read_from", this.contents.from()),
                left + 10, top + 40, 0xFFB0B0B0);

        graphics.fill(left + 8, top + 54, left + W - 8, top + 55, 0xFF606060);

        graphics.textWithWordWrap(this.font, Component.literal(this.contents.message()),
                left + 12, top + 64, W - 24, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}