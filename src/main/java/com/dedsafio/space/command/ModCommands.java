package com.dedsafio.space.command;

import com.dedsafio.space.bunker.ModState;
import com.dedsafio.space.config.ModConfig;
import com.dedsafio.space.supernova.SupernovaManager;
import com.dedsafio.space.supernova.SupernovaMode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.literal;

public final class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<ServerCommandSource> d) {
        Predicate<ServerCommandSource> admin = s -> s.hasPermissionLevel(ModConfig.get().commandPermissionLevel);

        d.register(literal("dedsafio")
            .then(literal("supernova").requires(admin)
                .then(literal("parte1").executes(c -> startSupernova(c, SupernovaMode.PART1)))
                .then(literal("parte2").executes(c -> startSupernova(c, SupernovaMode.PART2)))
                .then(literal("completa").executes(c -> startSupernova(c, SupernovaMode.FULL)))
                .then(literal("cancelar").executes(c -> {
                    SupernovaManager.cancel(c.getSource().getServer());
                    c.getSource().sendFeedback(() -> Text.literal("Secuencia cancelada."), true);
                    return 1;
                }))
                .then(literal("reiniciar").executes(c -> {
                    SupernovaManager.clearAll(c.getSource().getServer());
                    c.getSource().sendFeedback(() -> Text.literal("Estado de supernovas reiniciado (no se tocó ningún bloque)."), true);
                    return 1;
                })))
            .then(literal("bunker")
                .then(literal("asignar").requires(admin).executes(ModCommands::bunkerAssign))
                .then(literal("ver").executes(ModCommands::bunkerView))
                .then(literal("quitar").requires(admin).executes(ModCommands::bunkerRemove)))
            .then(literal("viaje").requires(admin)
                .then(literal("gleba").executes(c -> travel(c, true)))
                .then(literal("tierra").executes(c -> travel(c, false)))));
    }

    private static int startSupernova(CommandContext<ServerCommandSource> c, SupernovaMode mode) {
        if (!SupernovaManager.start(c.getSource().getServer(), mode)) {
            c.getSource().sendError(Text.literal("Ya hay una secuencia en curso. Usa /dedsafio supernova cancelar."));
            return 0;
        }
        c.getSource().sendFeedback(() -> Text.literal("Supernova iniciada: " + mode.name().toLowerCase()), true);
        return 1;
    }

    /** Solo registra la ubicación. NO coloca, reemplaza ni modifica ningún bloque. */
    private static int bunkerAssign(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        ModState st = ModState.get(c.getSource().getServer());
        st.bunker = new ModState.Point(p.getServerWorld().getRegistryKey().getValue(),
                p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
        st.markDirty();
        ModState.Point b = st.bunker;
        c.getSource().sendFeedback(() -> Text.literal(String.format(
                "Búnker asignado en %.1f %.1f %.1f (dimensión %s).", b.x(), b.y(), b.z(), b.dimension())), true);
        return 1;
    }

    private static int bunkerView(CommandContext<ServerCommandSource> c) {
        ModState.Point b = ModState.get(c.getSource().getServer()).bunker;
        if (b == null) {
            c.getSource().sendError(Text.literal("No hay búnker asignado. Entra en tu búnker y usa /dedsafio bunker asignar."));
            return 0;
        }
        c.getSource().sendFeedback(() -> Text.literal(String.format(
                "Búnker en %.1f %.1f %.1f (dimensión %s, yaw %.0f).", b.x(), b.y(), b.z(), b.dimension(), b.yaw())), false);
        return 1;
    }

    private static int bunkerRemove(CommandContext<ServerCommandSource> c) {
        ModState st = ModState.get(c.getSource().getServer());
        if (st.bunker == null) {
            c.getSource().sendError(Text.literal("No hay búnker asignado."));
            return 0;
        }
        st.bunker = null;
        st.markDirty();
        c.getSource().sendFeedback(() -> Text.literal("Asignación de búnker eliminada. No se modificó ningún bloque."), true);
        return 1;
    }

    private static int travel(CommandContext<ServerCommandSource> c, boolean toGleba) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        TravelService.Result r = toGleba ? TravelService.toGleba(p) : TravelService.toEarth(p);
        if (r.ok()) c.getSource().sendFeedback(() -> Text.literal(r.message()), true);
        else c.getSource().sendError(Text.literal(r.message()));
        return r.ok() ? 1 : 0;
    }

    private ModCommands() {}
}
