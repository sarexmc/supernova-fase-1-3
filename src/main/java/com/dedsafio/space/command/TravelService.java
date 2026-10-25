package com.dedsafio.space.command;

import com.dedsafio.space.bunker.ModState;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Viaje administrativo entre dimensiones. Comprueba que el destino esté cargado y sea seguro
 * ANTES de mover al jugador. Se ejecuta en el hilo del servidor.
 */
public final class TravelService {
    public static final RegistryKey<World> GLEBA =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier("dedsafio", "gleba"));

    public record Result(boolean ok, String message) {}

    public static Result toGleba(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return new Result(false, "Servidor no disponible.");
        ServerWorld target = server.getWorld(GLEBA);
        if (target == null) return new Result(false, "La dimensión dedsafio:gleba no está cargada.");

        ModState state = ModState.get(server);
        if (player.getServerWorld().getRegistryKey() != GLEBA) {
            state.returnPoints.put(player.getUuid(), new ModState.Point(
                    player.getServerWorld().getRegistryKey().getValue(),
                    player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
            state.markDirty();
        }
        Optional<BlockPos> safe = findSafe(target, 0, 0, 96, 8);
        if (safe.isEmpty()) return new Result(false, "No se encontró un punto seguro en Gleba.");
        BlockPos p = safe.get();
        player.teleport(target, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, player.getYaw(), 0f);
        return new Result(true, "Llegaste a Gleba en " + p.getX() + " " + p.getY() + " " + p.getZ());
    }

    public static Result toEarth(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return new Result(false, "Servidor no disponible.");
        ModState state = ModState.get(server);

        ServerWorld world = server.getOverworld();
        double x, z;
        float yaw = player.getYaw();
        ModState.Point ret = state.returnPoints.get(player.getUuid());
        if (ret != null) {
            ServerWorld w = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, ret.dimension()));
            if (w != null) world = w;
            x = ret.x();
            z = ret.z();
            yaw = ret.yaw();
        } else {
            BlockPos spawn = player.getSpawnPointPosition();
            if (spawn == null) spawn = world.getSpawnPos();
            x = spawn.getX();
            z = spawn.getZ();
        }
        Optional<BlockPos> safe = findSafe(world, (int) Math.floor(x), (int) Math.floor(z), 48, 2);
        if (safe.isEmpty()) return new Result(false, "No se encontró un punto seguro en el destino.");
        BlockPos p = safe.get();
        player.teleport(world, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0f);
        if (ret != null) {
            state.returnPoints.remove(player.getUuid());
            state.markDirty();
        }
        return new Result(true, "Regresaste a la Tierra en " + p.getX() + " " + p.getY() + " " + p.getZ());
    }

    /** Busca en anillos crecientes una columna con suelo sólido, sin fluidos y con dos bloques libres encima. */
    public static Optional<BlockPos> findSafe(ServerWorld world, int cx, int cz, int maxRadius, int step) {
        for (int r = 0; r <= maxRadius; r += step) {
            for (int dx = -r; dx <= r; dx += Math.max(step, 1)) {
                for (int dz = -r; dz <= r; dz += Math.max(step, 1)) {
                    if (r != 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue; // solo el anillo
                    Optional<BlockPos> p = checkColumn(world, cx + dx, cz + dz);
                    if (p.isPresent()) return p;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> checkColumn(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4); // carga/genera el chunk de forma síncrona
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 3) return Optional.empty();
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos below = feet.down();
        BlockState bs = world.getBlockState(below);
        boolean solidBelow = !bs.getCollisionShape(world, below).isEmpty() && world.getFluidState(below).isEmpty();
        boolean freeFeet = world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getFluidState(feet).isEmpty();
        BlockPos head = feet.up();
        boolean freeHead = world.getBlockState(head).getCollisionShape(world, head).isEmpty()
                && world.getFluidState(head).isEmpty();
        return (solidBelow && freeFeet && freeHead) ? Optional.of(feet) : Optional.empty();
    }

    private TravelService() {}
}
