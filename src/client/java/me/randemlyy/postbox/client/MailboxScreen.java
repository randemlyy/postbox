package me.randemlyy.postbox.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.randemlyy.postbox.net.MailboxNoticePayload;
import me.randemlyy.postbox.net.MailboxRequestPayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import me.randemlyy.postbox.net.ParcelEntry;
import me.randemlyy.postbox.net.SendParcelPayload;
import me.randemlyy.postbox.net.TakeParcelPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MailboxScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 236;
    private static final int ROW_HEIGHT = 20;
    private static final int ROWS_TOP = 44;
    private static final int NAMES_TOP = 124;
    private static final int NAME_ROW_HEIGHT = 16;
    private static final int MAX_VISIBLE_NAMES = 4;
    private static final long NOTICE_MILLIS = 5000L;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF909090;
    private static final int DIM = 0xFFB0B0B0;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;

    private enum Mode { INBOX, SEND }

    private OpenMailboxPayload data;
    private Mode mode = Mode.INBOX;
    private String recipientDraft = "";
    private String amountDraft = "";

    private EditBox recipientBox;
    private EditBox amountBox;
    private Button sendButton;

    private Component notice;
    private boolean noticeError;
    private long noticeUntil;

    public MailboxScreen(OpenMailboxPayload data) {
        super(Component.translatable("block.postbox.mailbox"));
        this.data = data;
    }

    public BlockPos pos() {
        return this.data.pos();
    }

    public void update(OpenMailboxPayload payload) {
        this.data = payload;
        this.rebuildWidgets();
        if (this.mode == Mode.SEND && this.recipientBox != null) {
            this.setInitialFocus(this.recipientBox);
        }
    }

    public void showNotice(MailboxNoticePayload payload) {
        showNotice(payload.key(), payload.arg(), payload.error());
    }

    private void showNotice(String key, String arg, boolean error) {
        this.notice = Component.translatable(key, arg);
        this.noticeError = error;
        this.noticeUntil = System.currentTimeMillis() + NOTICE_MILLIS;
    }

    @Override
    protected void init() {
        this.recipientBox = null;
        this.amountBox = null;
        this.sendButton = null;

        int left = panelLeft();
        int top = panelTop();
        int buttonY = top + PANEL_HEIGHT - 26;

        if (this.mode == Mode.INBOX) {
            addRenderableWidget(Button.builder(Component.translatable("postbox.screen.refresh"), button -> refresh())
                    .bounds(left + 10, buttonY, 88, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("postbox.screen.send"), button -> setMode(Mode.SEND))
                    .bounds(left + 106, buttonY, 88, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(left + 202, buttonY, 88, 20).build());
            return;
        }

        this.recipientBox = new EditBox(this.font, left + 36, top + 42, PANEL_WIDTH - 46, 18,
                Component.translatable("postbox.screen.to"));
        this.recipientBox.setMaxLength(16);
        this.recipientBox.setHint(Component.translatable("postbox.screen.to"));
        this.recipientBox.setValue(this.recipientDraft);
        this.recipientBox.setResponder(value -> this.recipientDraft = value);
        addRenderableWidget(this.recipientBox);

        addRenderableWidget(Button.builder(Component.literal("-"), button -> adjustAmount(-1))
                .bounds(left + 60, top + 66, 20, 18).build());

        this.amountBox = new EditBox(this.font, left + 84, top + 66, 44, 18,
                Component.translatable("postbox.screen.amount"));
        this.amountBox.setMaxLength(3);
        this.amountBox.setValue(initialAmountText());
        this.amountBox.setResponder(value -> this.amountDraft = value);
        addRenderableWidget(this.amountBox);

        addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustAmount(1))
                .bounds(left + 132, top + 66, 20, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("postbox.screen.back"), button -> setMode(Mode.INBOX))
                .bounds(left + 10, buttonY, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 106, buttonY, 88, 20).build());
        this.sendButton = addRenderableWidget(Button.builder(Component.translatable("postbox.screen.send_button"), button -> send())
                .bounds(left + 202, buttonY, 88, 20).build());
    }

    private void setMode(Mode mode) {
        this.mode = mode;
        this.rebuildWidgets();
    }

    private void refresh() {
        ClientPlayNetworking.send(new MailboxRequestPayload(this.data.pos()));
    }

    private void send() {
        if (heldStack().isEmpty()) {
            showNotice("postbox.notice.empty_hand", "", true);
            return;
        }

        String name = this.recipientBox.getValue().trim();
        if (name.isEmpty()) {
            showNotice("postbox.screen.no_recipient", "", true);
            return;
        }

        ClientPlayNetworking.send(new SendParcelPayload(this.data.pos(), name, plannedAmount()));
    }

    private void adjustAmount(int delta) {
        int available = heldStack().getCount();
        if (available <= 0 || this.amountBox == null) {
            return;
        }

        int next = Math.max(1, Math.min(plannedAmount() + delta, available));
        this.amountDraft = String.valueOf(next);
        this.amountBox.setValue(this.amountDraft);
    }

    private String initialAmountText() {
        int available = heldStack().getCount();
        if (available <= 0) {
            return "0";
        }

        int value;
        try {
            value = Integer.parseInt(this.amountDraft.trim());
        } catch (NumberFormatException e) {
            value = available;
        }
        return String.valueOf(Math.max(1, Math.min(value, available)));
    }

    private int plannedAmount() {
        int available = heldStack().getCount();
        if (available <= 0) {
            return 0;
        }

        String text = this.amountBox != null ? this.amountBox.getValue() : this.amountDraft;
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            value = available;
        }
        return Math.max(1, Math.min(value, available));
    }

    private List<String> visibleNames() {
        String query = this.recipientBox == null
                ? ""
                : this.recipientBox.getValue().trim().toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        for (String name : this.data.mailboxes()) {
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            out.add(name);
            if (out.size() >= MAX_VISIBLE_NAMES) {
                break;
            }
        }
        return out;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0181818);
        graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF808080);

        graphics.text(this.font,
                Component.translatable("postbox.mailbox.owner", this.data.ownerName()),
                left + 10, top + 10, WHITE);

        graphics.fill(left + 8, top + 38, left + PANEL_WIDTH - 8, top + 39, 0xFF606060);

        if (this.mode == Mode.INBOX) {
            drawInbox(graphics, left, top, mouseX, mouseY);
        } else {
            drawSend(graphics, left, top, mouseX, mouseY);
        }

        if (this.notice != null && System.currentTimeMillis() < this.noticeUntil) {
            graphics.text(this.font, this.notice, left + 10, top + 194,
                    this.noticeError ? RED : GREEN);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawInbox(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        graphics.text(this.font,
                Component.translatable("postbox.screen.parcels_waiting", this.data.totalCount()),
                left + 10, top + 24, DIM);

        if (this.data.parcels().isEmpty()) {
            graphics.text(this.font, Component.translatable("postbox.screen.empty"),
                    left + 10, top + ROWS_TOP, 0xFF808080);
            return;
        }

        for (int i = 0; i < this.data.parcels().size(); i++) {
            ParcelEntry entry = this.data.parcels().get(i);
            int rowY = top + ROWS_TOP + i * ROW_HEIGHT;

            if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                    && mouseY >= rowY - 2 && mouseY < rowY + ROW_HEIGHT - 2) {
                graphics.fill(left + 8, rowY - 2,
                        left + PANEL_WIDTH - 8, rowY + ROW_HEIGHT - 2, 0x30FFFFFF);
            }

            graphics.item(entry.stack(), left + 12, rowY);
            graphics.itemDecorations(this.font, entry.stack(), left + 12, rowY);

            graphics.text(this.font, entry.stack().getHoverName(),
                    left + 34, rowY + 4, WHITE);

            Component from = Component.translatable("postbox.screen.from", entry.senderName());
            graphics.text(this.font, from,
                    left + PANEL_WIDTH - 10 - this.font.width(from), rowY + 4, GRAY);
        }

        int hidden = this.data.totalCount() - this.data.parcels().size();
        if (hidden > 0) {
            graphics.text(this.font, Component.translatable("postbox.screen.more", hidden),
                    left + 10, top + ROWS_TOP + this.data.parcels().size() * ROW_HEIGHT + 2,
                    0xFF808080);
        }
    }

    private void drawSend(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        graphics.text(this.font, Component.translatable("postbox.screen.to"),
                left + 10, top + 47, DIM);
        graphics.text(this.font, Component.translatable("postbox.screen.amount"),
                left + 10, top + 71, DIM);

        ItemStack held = heldStack();
        if (held.isEmpty()) {
            graphics.text(this.font, Component.translatable("postbox.screen.hold_item"),
                    left + 10, top + 96, RED);
        } else {
            graphics.item(held, left + 10, top + 88);
            graphics.itemDecorations(this.font, held, left + 10, top + 88);
            graphics.text(this.font,
                    Component.translatable("postbox.screen.sending",
                            held.getHoverName(), plannedAmount(), held.getCount()),
                    left + 32, top + 92, DIM);
        }

        graphics.text(this.font, Component.translatable("postbox.screen.mailboxes"),
                left + 10, top + 112, 0xFF808080);

        List<String> names = visibleNames();
        if (names.isEmpty()) {
            graphics.text(this.font, Component.translatable("postbox.screen.no_mailboxes"),
                    left + 10, top + NAMES_TOP + 3, 0xFF707070);
        } else {
            for (int i = 0; i < names.size(); i++) {
                int rowY = top + NAMES_TOP + i * NAME_ROW_HEIGHT;
                if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                        && mouseY >= rowY && mouseY < rowY + NAME_ROW_HEIGHT) {
                    graphics.fill(left + 8, rowY,
                            left + PANEL_WIDTH - 8, rowY + NAME_ROW_HEIGHT, 0x30FFFFFF);
                }
                graphics.text(this.font, Component.literal(names.get(i)),
                        left + 12, rowY + 3, WHITE);
            }
        }

        if (this.sendButton != null) {
            this.sendButton.active = !held.isEmpty()
                    && !this.recipientBox.getValue().trim().isEmpty()
                    && plannedAmount() > 0;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() != 0 && event.button() != 1) {
            return false;
        }

        int left = panelLeft();
        int top = panelTop();
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (this.mode == Mode.INBOX) {
            for (int i = 0; i < this.data.parcels().size(); i++) {
                int rowY = top + ROWS_TOP + i * ROW_HEIGHT;
                if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                        && mouseY >= rowY - 2 && mouseY < rowY + ROW_HEIGHT - 2) {
                    ClientPlayNetworking.send(new TakeParcelPayload(this.data.pos(), i));
                    return true;
                }
            }
            return false;
        }

        List<String> names = visibleNames();
        for (int i = 0; i < names.size(); i++) {
            int rowY = top + NAMES_TOP + i * NAME_ROW_HEIGHT;
            if (mouseX >= left + 8 && mouseX < left + PANEL_WIDTH - 8
                    && mouseY >= rowY && mouseY < rowY + NAME_ROW_HEIGHT) {
                this.recipientDraft = names.get(i);
                this.recipientBox.setValue(this.recipientDraft);
                this.setInitialFocus(this.recipientBox);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && this.mode == Mode.SEND) {
            setMode(Mode.INBOX);
            return true;
        }
        if (super.keyPressed(event)) {
            return true;
        }
        if (this.mode == Mode.SEND && event.isConfirmation()) {
            send();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private static ItemStack heldStack() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack main = player.getMainHandItem();
        return main.isEmpty() ? player.getOffhandItem() : main;
    }
}