package me.randemlyy.postbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.phys.Vec3;

public final class ParrotTheatre {

    private static final List<Flight> FLIGHTS = new ArrayList<>();

    private record Flight(Parrot parrot, Vec3 from, Vec3 to, long start, long end) {}

    private ParrotTheatre() {}

    public static void depart(ServerPlayer player) {
        Vec3 from = player.getEyePosition().add(0.0, -0.3, 0.0);
        Vec3 to = from.add(player.getLookAngle().scale(5.0)).add(0.0, 4.0, 0.0);

        spawn(player, from, to, 40L);
    }

    public static void arrive(ServerPlayer player) {
        Vec3 to = player.getEyePosition().add(0.0, -0.4, 0.0);
        Vec3 from = to.add(2.0, 5.0, 2.0);

        spawn(player, from, to, 30L);
    }

    private static void spawn(ServerPlayer player, Vec3 from, Vec3 to, long duration) {
        Parrot parrot = EntityTypes.PARROT.create(player.level(), EntitySpawnReason.TRIGGERED);

        if (parrot == null) {
            return;
        }

        parrot.setNoAi(true);
        parrot.setNoGravity(true);
        parrot.setSilent(true);
        parrot.setPos(from.x, from.y, from.z);
        parrot.setYRot(heading(from, to));

        player.level().addFreshEntity(parrot);

        long now = player.level().getLevelData().getGameTime();
        FLIGHTS.add(new Flight(parrot, from, to, now, now + duration));
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
                f.parrot().discard();
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
