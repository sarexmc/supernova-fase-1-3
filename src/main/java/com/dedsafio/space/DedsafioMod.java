package com.dedsafio.space;

import com.dedsafio.space.command.ModCommands;
import com.dedsafio.space.config.ModConfig;
import com.dedsafio.space.supernova.SupernovaManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DedsafioMod implements ModInitializer {
    public static final String MOD_ID = "dedsafio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();
        ModSounds.register();
        ModCommands.register();
        SupernovaManager.init();
        LOGGER.info("Dedsafio Space inicializado (fase 1-3: comandos, búnker, supernovas).");
    }
}
