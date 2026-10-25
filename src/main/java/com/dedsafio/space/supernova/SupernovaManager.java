package com.dedsafio.space.supernova;

import com.dedsafio.space.DedsafioMod;
import com.dedsafio.space.bunker.ModState;
import com.dedsafio.space.config.ModConfig;
import com.dedsafio.space.net.ModNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Máquina de estados de las supernovas. UNICA fuente de verdad (servidor).
 * El cliente solo recibe el estado y avanza su contador local entre sincronizaciones.
 * No modifica bloques del mundo.
 */
public final class SupernovaManager {
    private static SupernovaPhase phase = SupernovaPhase.IDLE;
    private static SupernovaMode mode = SupernovaMode.PART1;
    private static int ticks = 0;
    private static int duration = 0;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SupernovaManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player, server));
        // Nunca arrastrar una secuencia a medias entre sesiones.
        ServerLifecycleEvents.SERVER_STARTED.register(s -> reset());
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> reset());
    }

    private static void reset() { phase = SupernovaPhase.IDLE; ticks = 0; duration = 0; }

    public static boolean isActive() { return phase != SupernovaPhase.IDLE; }
    public static SupernovaPhase phase() { return phase; }

    /** @return false si ya hay una secuencia en curso (evita reinicios accidentales). */
    public static boolean start(MinecraftServer server, SupernovaMode m) {
        if (isActive()) return false;
        ModState st = ModState.get(server);
        if (m == SupernovaMode.PART2) st.part2Done = false; else { st.part1Done = false; st.part2Done = false; }
        st.markDirty();
        mode = m;
        enter(server, m == SupernovaMode.PART2 ? SupernovaPhase.P2_COUNTDOWN : SupernovaPhase.P1_COUNTDOWN);
        return true;
    }

    /** Cancela la secuencia sin borrar el estado de "explotadas". */
    public static void cancel(MinecraftServer server) {
        reset();
        broadcastSync(server);
    }

    /** Cancela y borra el estado de supernovas explotadas. */
    public static void clearAll(MinecraftServer server) {
        reset();
        ModState st = ModState.get(server);
        st.part1Done = false;
        st.part2Done = false;
        st.markDirty();
        broadcastSync(server);
    }

    private static void tick(MinecraftServer server) {
        if (phase == SupernovaPhase.IDLE) return;
        ticks++;
        if (ticks >= duration) {
            advance(server);
        } else if (ticks % 20 == 0) {
            broadcastSync(server); // resincroniza el contador local de los clientes
        }
    }

    private static void advance(MinecraftServer server) {
        switch (phase) {
            case P1_COUNTDOWN -> enter(server, SupernovaPhase.P1_EXPLOSION);
            case P1_EXPLOSION -> enter(server, SupernovaPhase.P1_AFTERMATH);
            case P1_AFTERMATH -> {
                if (mode == SupernovaMode.FULL) enter(server, SupernovaPhase.BETWEEN); else finish(server);
            }
            case BETWEEN -> enter(server, SupernovaPhase.P2_COUNTDOWN);
            case P2_COUNTDOWN -> enter(server, SupernovaPhase.P2_EXPLOSION);
            case P2_EXPLOSION -> enter(server, SupernovaPhase.P2_AFTERMATH);
            case P2_AFTERMATH -> finish(server);
            default -> reset();
        }
    }

    private static void finish(MinecraftServer server) {
        reset();
        broadcastSync(server);
        DedsafioMod.LOGGER.info("Secuencia de supernova finalizada.");
    }

    private static void enter(MinecraftServer server, SupernovaPhase next) {
        phase = next;
        ticks = 0;
        duration = next.ticks(ModConfig.get());
        ModState st = ModState.get(server);
        ModConfig cfg = ModConfig.get();

        switch (next) {
            case P1_COUNTDOWN -> {
                server.getPlayerManager().broadcast(Text.literal("La estrella colapsará en "
                        + cfg.supernova1CountdownSeconds + " segundos. ¡Busca refugio inmediatamente!")
                        .formatted(Formatting.RED, Formatting.BOLD), false);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendBunkerHint(p, st);
            }
            case P2_COUNTDOWN ->
                server.getPlayerManager().broadcast(Text.literal("La supernova explotará en "
                        + cfg.supernova2CountdownSeconds + " segundos. ¡Aléjate!")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            case P1_AFTERMATH -> { st.part1Done = true; st.markDirty(); }
            case P2_AFTERMATH -> { st.part2Done = true; st.markDirty(); }
            default -> { }
        }
        broadcastSync(server);
    }

    private static void sendBunkerHint(ServerPlayerEntity p, ModState st) {
        if (st.bunker != null) {
            ModState.Point b = st.bunker;
            p.sendMessage(Text.literal(String.format("Refugio asignado: %.0f %.0f %.0f (%s). Dirígete allí por tus propios medios.",
                    b.x(), b.y(), b.z(), b.dimension())).formatted(Formatting.YELLOW));
        } else if (p.hasPermissionLevel(ModConfig.get().commandPermissionLevel)) {
            p.sendMessage(Text.literal("No hay refugio asignado. Construye uno, entra y usa /dedsafio bunker asignar.")
                    .formatted(Formatting.GRAY));
        }
    }

    private static void broadcastSync(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sync(p, server);
    }

    private static void sync(ServerPlayerEntity player, MinecraftServer server) {
        ModState st = ModState.get(server);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(phase.ordinal());
        buf.writeVarInt(ticks);
        buf.writeVarInt(duration);
        buf.writeVarInt(mode.ordinal());
        buf.writeBoolean(st.part1Done);
        buf.writeBoolean(st.part2Done);
        ServerPlayNetworking.send(player, ModNetworking.SUPERNOVA_SYNC, buf);
    }

    private SupernovaManager() {}
}
