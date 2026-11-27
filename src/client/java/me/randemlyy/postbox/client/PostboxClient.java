package me.randemlyy.postbox.client;

import me.randemlyy.postbox.net.MailboxNoticePayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

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
	}
}