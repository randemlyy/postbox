package me.randemlyy.postbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class ParcelWriter {

    private static final long TIMEOUT_TICKS = 20L * 60L;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Session(InteractionHand hand, long expiresAt) {}

    private ParcelWriter() {}

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, params) -> handleChat(message.signedContent(), sender)
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static boolean isWriting(UUID id) {
        return SESSIONS.containsKey(id);
    }

    public static void begin(ServerPlayer player, InteractionHand hand) {
        if (LetterWriter.isWriting(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("postbox.chat.letter_in_progress")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long expiresAt = player.level().getLevelData().getGameTime() + TIMEOUT_TICKS;
        SESSIONS.put(player.getUUID(), new Session(hand, expiresAt));

        player.sendSystemMessage(Component.translatable("postbox.chat.parcel_who")
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(mailboxList(player.level().getServer()));
    }

    private static boolean handleChat(String text, ServerPlayer sender) {
        Session session = SESSIONS.get(sender.getUUID());
        if (session == null) {
            return true;
        }

        long now = sender.level().getLevelData().getGameTime();
        if (now > session.expiresAt()) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.translatable("postbox.chat.parcel_expired")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (text.equalsIgnoreCase("cancel")) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.translatable("postbox.chat.parcel_cancelled")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        MailboxDirectory directory = MailboxDirectory.of(sender.level().getServer());
        MailboxDirectory.Entry target = directory.byName(text).orElse(null);

        if (target == null) {
            sender.sendSystemMessage(Component.translatable("postbox.notice.no_mailbox", text)
                    .withStyle(ChatFormatting.RED));
            sender.sendSystemMessage(mailboxList(sender.level().getServer()));
            sender.sendSystemMessage(Component.translatable("postbox.chat.type_name_or_cancel")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (target.ownerId().equals(sender.getUUID())) {
            sender.sendSystemMessage(Component.translatable("postbox.notice.self")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ParcelData parcels = ParcelData.of(sender.level().getServer());

        if (parcels.forRecipient(target.ownerId()).size() >= ParcelData.MAX_PER_RECIPIENT) {
            sender.sendSystemMessage(Component.translatable("postbox.notice.full", target.ownerName())
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ParcelPost.Result result = ParcelPost.send(sender, session.hand(), text, sender.getItemInHand(session.hand()).getCount());

        switch (result.outcome()) {

            case SENT -> {
                SESSIONS.remove(sender.getUUID());
                sender.sendSystemMessage(Component.translatable("postbox.notice.sent", result.recipientName()).withStyle(ChatFormatting.GOLD));
            }

            case EMPTY_HAND -> {
                SESSIONS.remove(sender.getUUID());
                sender.sendSystemMessage(Component.translatable("postbox.notice.empty_hand")
                        .withStyle(ChatFormatting.RED));
            }

            case NO_MAILBOX -> {
                sender.sendSystemMessage(Component.translatable("postbox.notice.no_mailbox", text)
                        .withStyle(ChatFormatting.RED));
                sender.sendSystemMessage(mailboxList(sender.level().getServer()));
                sender.sendSystemMessage(Component.translatable("postbox.chat.type_name_or_cancel")
                        .withStyle(ChatFormatting.GRAY));
            }

            case SELF -> sender.sendSystemMessage(Component.translatable("postbox.notice.self")
                    .withStyle(ChatFormatting.RED));

            case FULL -> sender.sendSystemMessage(
                    Component.translatable("postbox.notice.full", result.recipientName())
                            .withStyle(ChatFormatting.RED));
        }

        return false;
    }

    public static boolean cancel(Player player) {
        return SESSIONS.remove(player.getUUID()) != null;
    }

    private static Component mailboxList(MinecraftServer server) {
        List<String> names = MailboxDirectory.of(server).names();

        MutableComponent line = Component.translatable("postbox.chat.mailboxes").withStyle(ChatFormatting.GRAY);
        if (names.isEmpty()) {
            line.append(Component.translatable("postbox.chat.none_yet"));
            return line;
        }

        for (String name : names) {
            line.append(Component.literal(name)
                    .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(name))));
            line.append(Component.literal(" "));
        }
        return line;
    }
}