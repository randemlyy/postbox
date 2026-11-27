package me.randemlyy.postbox.client;

import me.randemlyy.postbox.net.MailboxNoticePayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import me.randemlyy.postbox.LetterContents;
import me.randemlyy.postbox.LetterItem;
import me.randemlyy.postbox.net.SealLetterPayload;
import me.randemlyy.postbox.net.SealLetterResultPayload;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public class PostboxClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(OpenMailboxPayload.TYPE, (payload, context) -> {
			Minecraft minecraft = Minecraft.getInstance();

			if (payload.initial()) {
				minecraft.setScreenAndShow(new MailboxScreen(payload));
				return;
			}

			if (minecraft.gui.screen() instanceof MailboxScreen screen && screen.pos().equals(payload.pos())) {
				screen.update(payload);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(MailboxNoticePayload.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof MailboxScreen screen) {
				screen.showNotice(payload);
			}
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof LetterItem) || !ClientPlayNetworking.canSend(SealLetterPayload.TYPE)) {
				return InteractionResult.PASS;
			}

			LetterContents contents = LetterContents.from(stack);
			Minecraft.getInstance().setScreenAndShow(contents != null ? new LetterReadScreen(contents) : new LetterComposeScreen(hand == InteractionHand.MAIN_HAND));

			return InteractionResult.SUCCESS;
		});

		ClientPlayNetworking.registerGlobalReceiver(SealLetterResultPayload.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof LetterComposeScreen screen) {
				screen.showResult(payload);
			}
		});
	}
}