package me.randemlyy.postbox;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class MailboxData extends SavedData {

    public static final Codec<MailboxData> CODEC = HeldMail.CODEC.listOf().xmap(MailboxData::new, MailboxData::entries);

    public static final SavedDataType<MailboxData> TYPE = new SavedDataType<>(
            Postbox.id("mailbox"),
            MailboxData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final List<HeldMail> entries = new ArrayList<>();

    public MailboxData() {}

    private MailboxData(List<HeldMail> loaded) {
        entries.addAll(loaded);
    }

    public List<HeldMail> entries() {
        return entries;
    }
}
