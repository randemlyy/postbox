package me.randemlyy.postbox.client;

import me.randemlyy.postbox.net.MailboxRequestPayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import me.randemlyy.postbox.net.ParcelEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MailboxScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 210;
    private static final int ROW_HEIGHT = 20;
    private static final int ROWS_TOP = 44;

    private final OpenMailboxPayload data;

    public MailboxScreen(OpenMailboxPayload data) {
        super(Component.translatable("block.postbox.mailbox"));
        this.data = data;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int buttonY = top + PANEL_HEIGHT - 26;

        addRenderableWidget(Button.builder(Component.translatable("postbox.screen.refresh"), button -> refresh())
                .bounds(left + 10, buttonY, 100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 110, buttonY, 100, 20)
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
                Component.translatable("postbox.screen.parcels_waiting", this.data.totalCount()),
                left + 10, top + 24, 0xFFB0B0B0);

        graphics.fill(left + 8, top + 38, left + PANEL_WIDTH - 8, top + 39, 0xFF606060);

        if (this.data.parcels().isEmpty()) {
            graphics.text(this.font, Component.translatable("postbox.screen.empty"),
                    left + 10, top + ROWS_TOP, 0xFF808080);
        } else {
            for (int i = 0; i < this.data.parcels().size(); i++) {
                ParcelEntry entry = this.data.parcels().get(i);
                int rowY = top + ROWS_TOP + i * ROW_HEIGHT;

                graphics.item(entry.stack(), left + 12, rowY);
                graphics.itemDecorations(this.font, entry.stack(), left + 12, rowY);

                graphics.text(this.font, entry.stack().getHoverName(),
                        left + 34, rowY + 4, 0xFFFFFFFF);

                Component from = Component.translatable("postbox.screen.from", entry.senderName());
                graphics.text(this.font, from,
                        left + PANEL_WIDTH - 10 - this.font.width(from), rowY + 4, 0xFF909090);
            }

            int hidden = this.data.totalCount() - this.data.parcels().size();
            if (hidden > 0) {
                graphics.text(this.font, Component.translatable("postbox.screen.more", hidden),
                        left + 10, top + ROWS_TOP + this.data.parcels().size() * ROW_HEIGHT + 2,
                        0xFF808080);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}