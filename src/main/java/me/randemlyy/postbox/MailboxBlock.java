package me.randemlyy.postbox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;

public class MailboxBlock extends Block implements EntityBlock {

    public MailboxBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (!(placer instanceof Player player)) return;
        if (!(level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox)) return;
        if (mailbox.ownerId() != null) return;
        mailbox.setOwner(player.getUUID(), player.getName().getString());

        if (level instanceof ServerLevel serverLevel) {
            MailboxDirectory.of(serverLevel.getServer()).register(serverLevel.dimension(), pos, player.getUUID(), player.getName().getString());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
            if (mailbox.ownerId() == null) {
                player.sendSystemMessage(Component.translatable("postbox.mailbox.unowned"));
            } else {
                player.sendSystemMessage(Component.translatable("postbox.mailbox.owner", mailbox.ownerName()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    protected void affectNeighbourAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        MailboxDirectory.of(level.getServer()).unregister(level.dimension(), pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox)) {
            return InteractionResult.PASS;
        }

        if (mailbox.ownerId() == null) {
            player.sendSystemMessage(Component.translatable("postbox.mailbox.unowned"));
            return InteractionResult.SUCCESS;
        }

        if (!mailbox.ownerId().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("postbox.mailbox.not_yours", mailbox.ownerName()));
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ParcelWriter.begin(serverPlayer, hand);
        }
        return InteractionResult.SUCCESS;
    }
}