package com.dedsafio.space.client;

import com.dedsafio.space.ModSounds;
import com.dedsafio.space.supernova.SupernovaMode;
import com.dedsafio.space.supernova.SupernovaPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;

import java.util.HashSet;
import java.util.Set;

/** Réplica del estado del servidor. El servidor manda; el cliente solo avanza su contador entre sincronizaciones. */
public final class SupernovaClientState {
    public static SupernovaPhase phase = SupernovaPhase.IDLE;
    public static SupernovaMode mode = SupernovaMode.PART1;
    public static int ticks = 0;
    public static int duration = 0;
    public static boolean part1Done = false;
    public static boolean part2Done = false;

    // Valores suavizados (evitan cortes abruptos)
    public static float gray = 0f;
    public static float dark = 0f;
    public static float red = 0f;

    private static final Set<String> played = new HashSet<>();

    public static void apply(int phaseOrd, int t, int dur, int modeOrd, boolean p1, boolean p2) {
        SupernovaPhase[] phases = SupernovaPhase.values();
        SupernovaMode[] modes = SupernovaMode.values();
        if (phaseOrd < 0 || phaseOrd >= phases.length || modeOrd < 0 || modeOrd >= modes.length) return;
        SupernovaPhase np = phases[phaseOrd];
        if (np != phase) played.clear();
        phase = np;
        mode = modes[modeOrd];
        ticks = t;
        duration = dur;
        part1Done = p1;
        part2Done = p2;
    }

    public static void reset() {
        phase = SupernovaPhase.IDLE;
        ticks = 0; duration = 0;
        gray = dark = red = 0f;
        part1Done = part2Done = false;
        played.clear();
    }

    public static float progress() { return duration <= 0 ? 0f : Math.min(1f, (float) ticks / duration); }
    public static int remainingTicks() { return Math.max(0, duration - ticks); }

    public static void tick() {
        if (phase != SupernovaPhase.IDLE && ticks < duration - 1) ticks++;

        float p = progress();
        float grayT = 0f, darkT = 0f, redT = 0f;
        switch (phase) {
            case P1_COUNTDOWN -> grayT = 0.55f * (float) Math.pow(p, 1.4);
            case P1_EXPLOSION -> darkT = 0.35f;
            case P1_AFTERMATH -> darkT = 0.50f - 0.20f * p;
            case BETWEEN -> darkT = 0.30f;
            case P2_COUNTDOWN -> redT = 0.14f + 0.06f * (float) Math.sin(ticks * 0.12);
            case P2_EXPLOSION -> darkT = 0.40f;
            case P2_AFTERMATH -> darkT = 0.58f - 0.22f * p;
            default -> { }
        }
        gray = approach(gray, grayT, 0.10f);
        dark = approach(dark, darkT, 0.02f);
        red = approach(red, redT, 0.02f);

        playScheduledSounds();
    }

    private static float approach(float cur, float target, float rate) {
        return cur + (target - cur) * rate;
    }

    private static void once(String key, int atTick, SoundEvent ev) {
        if (ticks >= atTick && ticks - atTick < 40 && played.add(key)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.getSoundManager().play(PositionedSoundInstance.master(ev, 1.0f, 1.0f));
        }
    }

    private static void playScheduledSounds() {
        switch (phase) {
            case P1_COUNTDOWN, P2_COUNTDOWN -> {
                once("star", 0, ModSounds.STAR_APPEAR);
                once("charge", duration - 240, ModSounds.ENERGY_CHARGE);
            }
            case P1_EXPLOSION -> {
                once("boom", 0, ModSounds.EXPLOSION_1);
                once("wave", 30, ModSounds.SHOCKWAVE);
            }
            case P2_EXPLOSION -> {
                once("boom", 0, ModSounds.EXPLOSION_2);
                once("flash", 0, ModSounds.FLASH);
                once("beam", 10, ModSounds.ENERGY_BEAM);
                once("wave", 40, ModSounds.SHOCKWAVE);
            }
            case P1_AFTERMATH, P2_AFTERMATH -> once("amb", 0, ModSounds.AFTERMATH);
            default -> { }
        }
    }

    public static void applyFromBuf(PacketByteBuf buf, MinecraftClient client) {
        int ph = buf.readVarInt(), t = buf.readVarInt(), d = buf.readVarInt(), m = buf.readVarInt();
        boolean p1 = buf.readBoolean(), p2 = buf.readBoolean();
        client.execute(() -> apply(ph, t, d, m, p1, p2));
    }

    private SupernovaClientState() {}
}
