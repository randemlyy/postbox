package me.randemlyy.postbox;

import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.parrot.Parrot;

public record ParrotIdentity(Parrot.Variant variant, Component customName,
                             boolean tame, UUID ownerId) {

    public static ParrotIdentity of(Parrot parrot) {
        EntityReference<?> owner = parrot.getOwnerReference();

        return new ParrotIdentity(
                parrot.getVariant(),
                parrot.getCustomName(),
                parrot.isTame(),
                owner == null ? null : owner.getUUID()
        );
    }

    public Parrot create(ServerLevel level) {
        Parrot parrot = EntityTypes.PARROT.create(level, EntitySpawnReason.TRIGGERED);

        if (parrot == null) {
            return null;
        }

        parrot.setComponent(DataComponents.PARROT_VARIANT, variant);

        if (customName != null) {
            parrot.setCustomName(customName);
        }

        if (tame) {
            parrot.setTame(true, false);

            if (ownerId != null) {
                parrot.setOwnerReference(EntityReference.of(ownerId));
            }
        }

        return parrot;
    }
}