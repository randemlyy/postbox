package me.randemlyy.postbox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;


public final class MailboxDirectory extends SavedData {

    public record Entry(UUID ownerId, String ownerName, ResourceKey<Level> dimension, BlockPos pos) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::ownerId),
                Codec.STRING.fieldOf("name").forGetter(Entry::ownerName),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos)
        ).apply(instance, Entry::new));
    }

    public static final Codec<MailboxDirectory> CODEC = Entry.CODEC.listOf().xmap(MailboxDirectory::new, MailboxDirectory::entries);

    public static final SavedDataType<MailboxDirectory> TYPE = new SavedDataType<>(
            Postbox.id("mailbox_directory"),
            MailboxDirectory::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final List<Entry> entries = new ArrayList<>();

    public MailboxDirectory() {}

    private MailboxDirectory(List<Entry> loaded) {
        entries.addAll(loaded);
    }

    public static MailboxDirectory of(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Entry> entries() {
        return entries;
    }

    public void register(ResourceKey<Level> dimension, BlockPos pos, UUID ownerID, String ownerName) {
        entries.removeIf(e -> e.dimension().equals(dimension) && e.pos().equals(pos));
        entries.add(new Entry(ownerID, ownerName, dimension, pos));
        setDirty();
    }

    public void unregister(ResourceKey<Level> dimension, BlockPos pos) {
        if (entries.removeIf(e -> e.dimension().equals(dimension) && e.pos().equals(pos))) {
            setDirty();
        }
    }

    public Optional<Entry> byName(String name) {
        return entries.stream().filter(e -> e.ownerName().equalsIgnoreCase(name)).findFirst();
    }

    public List<String> names() {
        Set<String> unique = new LinkedHashSet<>();
        for (Entry e : entries) {
            unique.add(e.ownerName());
        }
        List<String> sorted = new ArrayList<>(unique);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    public int mailboxCount(UUID ownerId) {
        return (int) entries.stream().filter(e -> e.ownerId().equals(ownerId)).count();
    }
}
