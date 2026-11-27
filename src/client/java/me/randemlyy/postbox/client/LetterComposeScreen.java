package me.randemlyy.postbox.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.randemlyy.postbox.net.SealLetterPayload;
import me.randemlyy.postbox.net.SealLetterResultPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class LetterComposeScreen extends Screen {

    private static final int W = 320;
    private static final int H = 268;
    private static final int NAMES_TOP = 64;
    private static final int NAME_ROW = 14;
    private static final int MAX_NAMES = 4;
    private static final int MAX_MESSAGE = 400;

    private final boolean mainHand;
    private String toDraft = "";
    private String bodyDraft = "";
    private EditBox toBox;
    private MultiLineEditBox bodyBox;
    private Button sealButton;
    private Component notice;

    public LetterComposeScreen(boolean mainHand) {
        super(Component.translatable("postbox.screen.letter"));
        this.mainHand = mainHand;
    }

    @Override
    protected void init() {
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;

        this.toBox = new EditBox(this.font, left + 34, top + 28, W - 44, 18,
                Component.translatable("postbox.screen.letter_to"));
        this.toBox.setMaxLength(16);
        this.toBox.setValue(this.toDraft);
        this.toBox.setResponder(value -> this.toDraft = value);
        addRenderableWidget(this.toBox);

        this.bodyBox = MultiLineEditBox.builder()
                .setX(left + 10)
                .setY(top + 126)
                .setPlaceholder(Component.translatable("postbox.screen.letter_body"))
                .build(this.font, W - 20, 84, Component.translatable("postbox.screen.letter_body"));
        this.bodyBox.setCharacterLimit(MAX_MESSAGE);
        this.bodyBox.setValueListener(value -> this.bodyDraft = value);
        this.bodyBox.setValue(this.bodyDraft);
        addRenderableWidget(this.bodyBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 10, top + 242, 90, 20).build());

        this.sealButton = addRenderableWidget(Button.builder(Component.translatable("postbox.screen.seal"),
                        button -> seal())
                .bounds(left + 220, top + 242, 90, 20).build());
    }

    public void showResult(SealLetterResultPayload payload) {
        if (payload.success()) {
            onClose();
            return;
        }
        this.notice = Component.translatable(payload.key(), payload.arg());
    }

    private void seal() {
        String to = this.toBox.getValue().trim();
        String body = this.bodyBox.getValue().trim();

        if (to.isEmpty()) {
            this.notice = Component.translatable("postbox.letter.need_name");
            return;
        }
        if (body.isEmpty()) {
            this.notice = Component.translatable("postbox.letter.empty");
            return;
        }

        ClientPlayNetworking.send(new SealLetterPayload(this.mainHand, to, body));
    }

    private List<String> visibleNames() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }

        String self = Minecraft.getInstance().player == null
                ? ""
                : Minecraft.getInstance().player.getScoreboardName();
        String query = this.toBox == null ? "" : this.toBox.getValue().trim().toLowerCase(Locale.ROOT);

        List<String> names = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name.equalsIgnoreCase(self)) {
                continue;
            }
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            names.add(name);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names.size() > MAX_NAMES ? names.subList(0, MAX_NAMES) : names;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;

        graphics.fill(left, top, left + W, top + H, 0xF0181818);
        graphics.outline(left, top, W, H, 0xFF808080);

        graphics.text(this.font, Component.translatable("postbox.screen.letter"), left + 10, top + 10, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("postbox.screen.letter_to"), left + 10, top + 33, 0xFFB0B0B0);
        graphics.text(this.font, Component.translatable("postbox.screen.letter_players"), left + 10, top + 52, 0xFF808080);

        List<String> names = visibleNames();
        for (int i = 0; i < names.size(); i++) {
            int rowY = top + NAMES_TOP + i * NAME_ROW;
            if (mouseX >= left + 8 && mouseX < left + W - 8 && mouseY >= rowY && mouseY < rowY + NAME_ROW) {
                graphics.fill(left + 8, rowY, left + W - 8, rowY + NAME_ROW, 0x30FFFFFF);
            }
            graphics.text(this.font, Component.literal(names.get(i)), left + 12, rowY + 2, 0xFFFFFFFF);
        }

        Component count = Component.translatable("postbox.screen.char_count",
                this.bodyBox.getValue().length(), MAX_MESSAGE);
        graphics.text(this.font, count, left + W - 10 - this.font.width(count), top + 214, 0xFF909090);

        if (this.notice != null) {
            graphics.text(this.font, this.notice, left + 10, top + 228, 0xFFFF5555);
        }

        this.sealButton.active = !this.toBox.getValue().trim().isEmpty()
                && !this.bodyBox.getValue().trim().isEmpty();

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }

        int left = (this.width - W) / 2;
        int top = (this.height - H) / 2;
        List<String> names = visibleNames();

        for (int i = 0; i < names.size(); i++) {
            int rowY = top + NAMES_TOP + i * NAME_ROW;
            if (event.x() >= left + 8 && event.x() < left + W - 8
                    && event.y() >= rowY && event.y() < rowY + NAME_ROW) {
                this.toDraft = names.get(i);
                this.toBox.setValue(this.toDraft);
                this.setInitialFocus(this.bodyBox);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() && this.toBox.isFocused()) {
            this.setInitialFocus(this.bodyBox);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}