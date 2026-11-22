package me.randemlyy.postbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.phys.Vec3;

public final class ParrotTheatre {

    private static final List<Flight> FLIGHTS = new ArrayList<>();

    public static final String COURIER_TAG = "postbox_courier";
    private record Flight(Parrot parrot, Vec3 from, Vec3 to, long start, long end, boolean stays) {}

    private ParrotTheatre() {}

    public static void depart(Parrot parrot, ServerPlayer player) {
        Vec3 from = parrot.position();
        Vec3 to = from.add(player.getLookAngle().scale(5.0)).add(0.0, 4.0, 0.0);

        parrot.setNoAi(true);
        parrot.setNoGravity(true);
        parrot.setSilent(true);
        parrot.setInvulnerableTime(60);
        parrot.addTag(COURIER_TAG);

        begin(parrot, from, to, 40L, false);
    }

    public static void arrive(ServerPlayer player) {
        Vec3 to = player.getEyePosition().add(0.0, -0.4, 0.0);
        Vec3 from = to.add(2.0, 5.0, 2.0);

        spawnArrival(player, from, to, 30L, false);
    }

    public static void returnTo(ServerPlayer player, ParrotIdentity identity) {
        Parrot parrot = identity.create(player.level());

        if (parrot == null) {
            return;
        }

        Vec3 to = player.getEyePosition().add(0.0, -0.4, 0.0);
        Vec3 from = to.add(2.0, 5.0, 2.0);

        parrot.setNoAi(true);
        parrot.setNoGravity(true);
        parrot.setSilent(true);
        parrot.setInvulnerableTime(60);
        parrot.addTag(COURIER_TAG);
        parrot.setPos(from.x, from.y, from.z);
        parrot.setYRot(heading(from, to));

        player.level().addFreshEntity(parrot);

        begin(parrot, from, to, 30L, true);   // stays = true
    }


    private static void begin(Parrot parrot, Vec3 from, Vec3 to, long duration, boolean stays) {
        parrot.setPos(from.x, from.y, from.z);
        parrot.setYRot(heading(from, to));

        long now = parrot.level().getLevelData().getGameTime();
        FLIGHTS.add(new Flight(parrot, from, to, now, now + duration, stays));
    }

    private static void spawnArrival(ServerPlayer player, Vec3 from, Vec3 to, long duration, boolean stays) {
        Parrot parrot = EntityTypes.PARROT.create(player.level(), EntitySpawnReason.TRIGGERED);

        if (parrot == null) {
            return;
        }

        parrot.setNoAi(true);
        parrot.setNoGravity(true);
        parrot.setSilent(true);
        parrot.setInvulnerableTime(60);
        parrot.addTag(COURIER_TAG);

        player.level().addFreshEntity(parrot);

        begin(parrot, from, to, duration, stays);
    }

    public static void tick(MinecraftServer server, long now) {
        Iterator<Flight> it = FLIGHTS.iterator();

        while (it.hasNext()) {
            Flight f = it.next();

            long duration = f.end() - f.start();
            double t = Math.min(1.0, (now - f.start()) / (double) duration);

            Vec3 pos = f.from().add(f.to().subtract(f.from()).scale(t));
            Vec3 step = f.to().subtract(f.from()).scale(1.0 / duration);

            f.parrot().setPos(pos.x, pos.y, pos.z);
            f.parrot().setDeltaMovement(step);

            if (now >= f.end()) {
                if (f.stays()) {
                    f.parrot().setNoAi(false);
                    f.parrot().setNoGravity(false);
                    f.parrot().setSilent(false);
                } else {
                    f.parrot().discard();
                }

                it.remove();
            }
        }
    }

    public static void reset() {
        for (Flight f: FLIGHTS) {
            f.parrot().discard();
        }

        FLIGHTS.clear();
    }

    private static float heading(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }
}
