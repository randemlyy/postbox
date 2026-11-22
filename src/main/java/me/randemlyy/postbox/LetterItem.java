package me.randemlyy.postbox;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;


public class LetterItem extends Item{

    public LetterItem(Properties properties){
        super(properties);
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        LetterWriter.useLetter(player, hand, stack);

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public @NonNull InteractionResult interactLivingEntity(@NonNull ItemStack stack, @NonNull Player player, @NonNull LivingEntity target, @NonNull InteractionHand hand) {

        // parrot check
        if (!(target instanceof Parrot)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer sender)) {
            return InteractionResult.PASS;
        }

        LetterContents contents = LetterContents.from(stack);

        if (contents == null) {
            sender.sendSystemMessage(Component.literal("This letter is currently empty, right-click the air to write")
                    .withStyle(ChatFormatting.RED));

            return InteractionResult.PASS;
        }

        MinecraftServer server = sender.level().getServer();

        ServerPlayer recipient = server.getPlayerList().getPlayer(contents.to());

        if (recipient == null) {
            sender.sendSystemMessage(Component.literal(contents.to() + " is not online. Letters need a parrot, and parrots need somewhere to land")
                    .withStyle(ChatFormatting.RED));

            return InteractionResult.PASS;
        }

        Courier.send(sender, recipient, stack);
        return InteractionResult.SUCCESS_SERVER;
    }
}
