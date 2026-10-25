package com.dedsafio.space;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent STAR_APPEAR = of("supernova.star_appear");
    public static final SoundEvent ENERGY_CHARGE = of("supernova.energy_charge");
    public static final SoundEvent EXPLOSION_1 = of("supernova.explosion_1");
    public static final SoundEvent SHOCKWAVE = of("supernova.shockwave");
    public static final SoundEvent AFTERMATH = of("supernova.aftermath");
    public static final SoundEvent EXPLOSION_2 = of("supernova.explosion_2");
    public static final SoundEvent FLASH = of("supernova.flash");
    public static final SoundEvent ENERGY_BEAM = of("supernova.energy_beam");

    private static SoundEvent of(String path) {
        Identifier id = new Identifier(DedsafioMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() { /* fuerza la carga de la clase */ }
    private ModSounds() {}
}
