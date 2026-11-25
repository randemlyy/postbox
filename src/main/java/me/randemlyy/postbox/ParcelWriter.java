package me.randemlyy.postbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public final class ParcelWriter {

    private static final long TIMEOUT_TICKS = 20L * 60L;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Session(InteractionHand hand, long expiresAt) {}

    private ParcelWriter() {}

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, params) -> handleChat(message.signedContent(), sender)
        );
    }

    public static boolean isWriting(UUID id) {
        return SESSIONS.containsKey(id);
    }

    public static void begin(ServerPlayer player, InteractionHand hand) {
        if (LetterWriter.isWriting(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Finish or cancel your letter first")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long expiresAt = player.level().getLevelData().getGameTime() + TIMEOUT_TICKS;
        SESSIONS.put(player.getUUID(), new Session(hand, expiresAt));

        player.sendSystemMessage(Component.literal("Who is this parcel for? Type a name, or cancel.")
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
            sender.sendSystemMessage(Component.literal("That parcel request expired")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (text.equalsIgnoreCase("cancel")) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.literal("Parcel cancelled")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        MailboxDirectory directory = MailboxDirectory.of(sender.level().getServer());
        MailboxDirectory.Entry target = directory.byName(text).orElse(null);

        if (target == null) {
            sender.sendSystemMessage(Component.literal("Nobody called \"" + text + "\" has a mailbox")
                    .withStyle(ChatFormatting.RED));
            sender.sendSystemMessage(mailboxList(sender.level().getServer()));
            sender.sendSystemMessage(Component.literal("Type another name or cancel")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (target.ownerId().equals(sender.getUUID())) {
            sender.sendSystemMessage(Component.literal("You cannot post a parcel to yourself")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ParcelData parcels = ParcelData.of(sender.level().getServer());

        if (parcels.forRecipient(target.ownerId()).size() >= ParcelData.MAX_PER_RECIPIENT) {
            sender.sendSystemMessage(Component.literal(target.ownerName() + "'s mailbox is full")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ItemStack held = sender.getItemInHand(session.hand());
        if (held.isEmpty()) {
            SESSIONS.remove(sender.getUUID());
            sender.sendSystemMessage(Component.literal("Your hand is empty - parcel cancelled")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        SESSIONS.remove(sender.getUUID());

        ItemStack contents = held.copy();
        if (!sender.hasInfiniteMaterials()) {
            held.shrink(contents.getCount());
        }

        parcels.entries().add(new Parcel(
                sender.getUUID(), sender.getScoreboardName(),
                target.ownerId(), target.ownerName(),
                contents, now));
        parcels.setDirty();

        sender.sendSystemMessage(Component.literal("Parcel sent to " + target.ownerName())
                .withStyle(ChatFormatting.GOLD));

        ServerPlayer online = sender.level().getServer().getPlayerList().getPlayer(target.ownerId());
        if (online != null && !online.getUUID().equals(sender.getUUID())) {
            online.sendSystemMessage(Component.literal("A parcel from " + sender.getScoreboardName()
                            + " is waiting in your mailbox")
                    .withStyle(ChatFormatting.GOLD));
        }

        return false;
    }

    private static Component mailboxList(MinecraftServer server) {
        List<String> names = MailboxDirectory.of(server).names();

        MutableComponent line = Component.literal("Mailboxes: ").withStyle(ChatFormatting.GRAY);
        if (names.isEmpty()) {
            line.append(Component.literal("(none yet)"));
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