package me.randemlyy.postbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.NonNull;


public final class LetterWriter {

    private enum Stage { ASK_NAME, ASK_MESSAGE }

    private static final long TIMEOUT_TICKS = 20L * 60L;
    public static final int MAX_MESSAGE_LENGTH = 400;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Session(InteractionHand hand, Stage stage, String recipientName, long expiresAt) {}

    private LetterWriter() {}

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, params) -> handleChat(message.signedContent(), sender)
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static void useLetter(Player player, InteractionHand hand, @NonNull ItemStack stack) {
        if (stack.hasNonDefault(DataComponents.LORE)) {
            read(player, stack);
            return;
        }

        if (SESSIONS.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("postbox.chat.already_letter")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (ParcelWriter.isWriting(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("postbox.chat.already_parcel")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long expiresAt = player.level().getLevelData().getGameTime() + TIMEOUT_TICKS;
        SESSIONS.put(player.getUUID(), new Session(hand, Stage.ASK_NAME, null, expiresAt));

        player.sendSystemMessage(Component.translatable("postbox.chat.letter_who")
                .withStyle(ChatFormatting.GOLD));
    }

    private static boolean handleChat(String text, @NonNull ServerPlayer sender) {
        Session session = SESSIONS.get(sender.getUUID());
        if (session == null) {
            return true;
        }

        long now = sender.level().getLevelData().getGameTime();
        if (now > session.expiresAt()) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.translatable("postbox.chat.letter_expired")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (text.equalsIgnoreCase("cancel")) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.translatable("postbox.chat.letter_discarded")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (session.stage() == Stage.ASK_NAME) {
            askMessage(sender, session, text);
        } else {
            finishLetter(sender, session, text);
        }

        return false;
    }

    private static void askMessage(@NonNull ServerPlayer sender, Session session, String name) {
        PlayerList players = sender.level().getServer().getPlayerList();
        ServerPlayer target = players.getPlayer(name);

        if (target == null) {
            sender.sendSystemMessage(Component.translatable("postbox.chat.not_online_named", name)
                    .withStyle(ChatFormatting.RED));
            sender.sendSystemMessage(onlineList(players));
            sender.sendSystemMessage(Component.translatable("postbox.chat.type_name_or_cancel")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        if (target.getUUID().equals(sender.getUUID())) {
            sender.sendSystemMessage(Component.translatable("postbox.letter.self")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long expiresAt = sender.level().getLevelData().getGameTime() + TIMEOUT_TICKS;
        SESSIONS.put(sender.getUUID(), new Session(session.hand(), Stage.ASK_MESSAGE, target.getScoreboardName(), expiresAt));

        sender.sendSystemMessage(Component.translatable("postbox.chat.letter_target", target.getScoreboardName())
                .withStyle(ChatFormatting.GOLD));
        sender.sendSystemMessage(Component.translatable("postbox.chat.slash_hint")
                .withStyle(ChatFormatting.GRAY));

    }

    private static @NonNull Component onlineList(@NonNull PlayerList players) {

        // Creates the list of online players, its clickable so it's easier to compose a letter quicker
        MutableComponent line = Component.translatable("postbox.chat.online").withStyle(ChatFormatting.GRAY);

        for (ServerPlayer p : players.getPlayers()) {
            String name = p.getScoreboardName();
            line.append(Component.literal(name + " ").withStyle(style -> style.withClickEvent((new ClickEvent.SuggestCommand(name)))));
        }

        return line;
    }

    private static void finishLetter(ServerPlayer sender, Session session, @NonNull String text) {

        // verifies max character limit of letter message
        if (text.length() > MAX_MESSAGE_LENGTH) {
            sender.sendSystemMessage(Component.translatable("postbox.chat.letter_too_long", MAX_MESSAGE_LENGTH)
                    .withStyle(ChatFormatting.RED));
            return;
        }

        ItemStack stack = sender.getItemInHand(session.hand());

        // checks whether the letter is still equipped by the player when finishing the letter
        if (!(stack.getItem() instanceof LetterItem)) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.translatable("postbox.chat.letter_not_in_hand")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        SESSIONS.remove(sender.getUUID());

        seal(sender, stack, session.recipientName(), text);

        sender.sendSystemMessage(Component.translatable("postbox.chat.letter_sealed")
                .withStyle(ChatFormatting.GREEN));
    }

    private static void read(Player player, @NonNull ItemStack stack) {
        LetterContents contents = LetterContents.from(stack);

        if (contents == null) {
            return;
        }

        player.sendSystemMessage(Component.translatable("postbox.chat.letter_header").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("postbox.letter.read_to", contents.to()));
        player.sendSystemMessage(Component.translatable("postbox.letter.read_from", contents.from()));
        player.sendSystemMessage(Component.literal(contents.message()));

    }

    public static void seal(ServerPlayer sender, ItemStack stack, String recipientName, String message) {
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(LetterContents.TO_PREFIX + recipientName),
                Component.literal(LetterContents.FROM_PREFIX + sender.getScoreboardName()),
                Component.literal(message))));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.postbox.letter.to", recipientName));
    }

    public static boolean cancel(Player player) {
        return SESSIONS.remove(player.getUUID()) != null;
    }

    public static boolean isWriting(UUID id) {
        return SESSIONS.containsKey(id);
    }
}
