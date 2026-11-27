package me.randemlyy.postbox;

import me.randemlyy.postbox.net.SealLetterPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;


public class LetterItem extends Item{

    public LetterItem(Properties properties){
        super(properties);
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer && ServerPlayNetworking.canSend(serverPlayer, SealLetterPayload.TYPE)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        LetterWriter.useLetter(player, hand, stack);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public @NonNull InteractionResult interactLivingEntity(@NonNull ItemStack stack, @NonNull Player player, @NonNull LivingEntity target, @NonNull InteractionHand hand) {

        // checks
        if (!(target instanceof Parrot parrot)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer sender)) {
            return InteractionResult.PASS;
        }

        // already flying
        if (parrot.entityTags().contains(ParrotTheatre.COURIER_TAG)) {
            return InteractionResult.PASS;
        }

        LetterContents contents = LetterContents.from(stack);

        if (contents == null) {
            sender.sendSystemMessage(Component.translatable("postbox.chat.letter_empty")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        MinecraftServer server = sender.level().getServer();

        ServerPlayer recipient = server.getPlayerList().getPlayer(contents.to());

        if (recipient == null) {
            sender.sendSystemMessage(Component.translatable("postbox.chat.letter_not_online", contents.to())
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        // tamed parrot check
        if (parrot.isTame() && !parrot.isOwnedBy(sender)) {
            sender.sendSystemMessage(Component.translatable("postbox.chat.parrot_owned")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        if (parrot.isPassenger()) {
            parrot.stopRiding();
        }

        Courier.send(sender, recipient, stack, parrot);
        return InteractionResult.SUCCESS_SERVER;
    }
}
