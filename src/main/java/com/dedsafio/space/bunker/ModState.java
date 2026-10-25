package com.dedsafio.space.bunker;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estado persistente del mod (se guarda en data/dedsafio_state.dat del mundo).
 * Contiene SOLO datos propios: no modifica bloques ni datos vanilla.
 */
public class ModState extends PersistentState {
    public record Point(Identifier dimension, double x, double y, double z, float yaw, float pitch) {}

    public Point bunker = null;
    public boolean part1Done = false;
    public boolean part2Done = false;
    /** Punto de regreso a la Tierra por jugador (donde estaba al viajar a Gleba). */
    public final Map<UUID, Point> returnPoints = new HashMap<>();

    public static ModState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(ModState::read, ModState::new, "dedsafio_state");
    }

    private static NbtCompound writePoint(Point p) {
        NbtCompound n = new NbtCompound();
        n.putString("dim", p.dimension().toString());
        n.putDouble("x", p.x());
        n.putDouble("y", p.y());
        n.putDouble("z", p.z());
        n.putFloat("yaw", p.yaw());
        n.putFloat("pitch", p.pitch());
        return n;
    }

    private static Point readPoint(NbtCompound n) {
        Identifier dim = Identifier.tryParse(n.getString("dim"));
        if (dim == null) return null;
        return new Point(dim, n.getDouble("x"), n.getDouble("y"), n.getDouble("z"),
                n.getFloat("yaw"), n.getFloat("pitch"));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (bunker != null) nbt.put("bunker", writePoint(bunker));
        nbt.putBoolean("part1Done", part1Done);
        nbt.putBoolean("part2Done", part2Done);
        NbtCompound ret = new NbtCompound();
        returnPoints.forEach((id, p) -> ret.put(id.toString(), writePoint(p)));
        nbt.put("returns", ret);
        return nbt;
    }

    public static ModState read(NbtCompound nbt) {
        ModState s = new ModState();
        if (nbt.contains("bunker")) s.bunker = readPoint(nbt.getCompound("bunker"));
        s.part1Done = nbt.getBoolean("part1Done");
        s.part2Done = nbt.getBoolean("part2Done");
        NbtCompound ret = nbt.getCompound("returns");
        for (String key : ret.getKeys()) {
            try {
                Point p = readPoint(ret.getCompound(key));
                if (p != null) s.returnPoints.put(UUID.fromString(key), p);
            } catch (IllegalArgumentException ignored) { }
        }
        return s;
    }
}
