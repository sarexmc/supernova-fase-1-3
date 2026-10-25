package com.dedsafio.space.config;

import com.dedsafio.space.DedsafioMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuración del servidor: config/dedsafio.json. Nunca toca datos del mundo. */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance = new ModConfig();

    public int supernova1CountdownSeconds = 60;
    public int supernova2CountdownSeconds = 60;
    public int explosion1Ticks = 120;
    public int aftermath1Ticks = 400;
    public int betweenTicks = 200;
    public int explosion2Ticks = 200;
    public int aftermath2Ticks = 600;
    /** Nivel de permiso para comandos administrativos (asignar/quitar búnker, supernovas, viaje). */
    public int commandPermissionLevel = 2;
    /** 0 = bajo, 1 = medio, 2 = alto. Lo usará el cliente en las fases de VFX. */
    public int vfxDetail = 2;

    public static ModConfig get() { return instance; }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("dedsafio.json");
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                ModConfig loaded = GSON.fromJson(r, ModConfig.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                DedsafioMod.LOGGER.error("No se pudo leer dedsafio.json, se usan valores por defecto", e);
            }
        }
        instance.clamp();
        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            DedsafioMod.LOGGER.error("No se pudo escribir dedsafio.json", e);
        }
    }

    private void clamp() {
        supernova1CountdownSeconds = Math.max(5, Math.min(600, supernova1CountdownSeconds));
        supernova2CountdownSeconds = Math.max(5, Math.min(600, supernova2CountdownSeconds));
        explosion1Ticks = Math.max(40, Math.min(1200, explosion1Ticks));
        explosion2Ticks = Math.max(60, Math.min(1200, explosion2Ticks));
        aftermath1Ticks = Math.max(20, Math.min(6000, aftermath1Ticks));
        aftermath2Ticks = Math.max(20, Math.min(6000, aftermath2Ticks));
        betweenTicks = Math.max(0, Math.min(6000, betweenTicks));
        commandPermissionLevel = Math.max(0, Math.min(4, commandPermissionLevel));
        vfxDetail = Math.max(0, Math.min(2, vfxDetail));
    }
}
