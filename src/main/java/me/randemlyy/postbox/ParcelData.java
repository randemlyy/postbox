package me.randemlyy.postbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ParcelData extends SavedData {

    public static final Codec<ParcelData> CODEC = Parcel.CODEC.listOf().xmap(ParcelData::new, ParcelData::entries);
    public static final int MAX_PER_RECIPIENT = 64;

    public static final SavedDataType<ParcelData> TYPE = new SavedDataType<>(
            Postbox.id("parcels"),
            ParcelData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final List<Parcel> entries = new ArrayList<>();

    public ParcelData() {}

    private ParcelData(List<Parcel> loaded) {
        entries.addAll(loaded);
    }

    public static ParcelData of(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Parcel> entries() {
        return entries;
    }

    public List<Parcel> forRecipient(UUID recipientId) {
        List<Parcel> out = new ArrayList<>();
        for (Parcel parcel : entries) {
            if (parcel.recipientId().equals(recipientId)) {
                out.add(parcel);
            }
        }
        return out;
    }

    public Optional<Parcel> takeOldest(UUID recipientId) {
        Iterator<Parcel> it = entries.iterator();
        while (it.hasNext()) {
            Parcel parcel = it.next();
            if (parcel.recipientId().equals(recipientId)) {
                it.remove();
                setDirty();
                return Optional.of(parcel);
            }
        }
        return Optional.empty();
    }
}