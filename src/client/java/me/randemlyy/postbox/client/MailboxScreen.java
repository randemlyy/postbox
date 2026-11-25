package me.randemlyy.postbox.client;

import me.randemlyy.postbox.net.MailboxRequestPayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MailboxScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 160;

    private final OpenMailboxPayload data;

    public MailboxScreen(OpenMailboxPayload data) {
        super(Component.translatable("block.postbox.mailbox"));
        this.data = data;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        addRenderableWidget(Button.builder(Component.translatable("postbox.screen.refresh"), button -> refresh())
                .bounds(left + 10, top + PANEL_HEIGHT - 30, 100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 110, top + PANEL_HEIGHT - 30, 100, 20)
                .build());
    }

    private void refresh() {
        ClientPlayNetworking.send(new MailboxRequestPayload(this.data.pos()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0181818);
        graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF808080);

        graphics.text(this.font,
                Component.translatable("postbox.mailbox.owner", this.data.ownerName()),
                left + 10, top + 10, 0xFFFFFFFF);

        graphics.text(this.font,
                Component.translatable("postbox.screen.parcels_waiting", this.data.parcelCount()),
                left + 10, top + 26, 0xFFB0B0B0);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}