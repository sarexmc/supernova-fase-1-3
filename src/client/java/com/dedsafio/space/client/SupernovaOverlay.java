package com.dedsafio.space.client;

import com.dedsafio.space.supernova.SupernovaPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Capa 2D provisional (Fase 2): gris progresivo, tinte rojo, destellos, rayo lateral y cuenta atrás.
 * El cielo/estrella en 3D, los shaders de desaturación real y el desenfoque llegan en la Fase 4.
 */
public final class SupernovaOverlay {

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        SupernovaPhase ph = SupernovaClientState.phase;
        float t = SupernovaClientState.ticks + tickDelta;

        // Gris suavizado (aproximación a la pérdida de saturación)
        layer(ctx, w, h, 0x8A8A8A, SupernovaClientState.gray);
        // Tinte rojo moderado (cielo de la parte 2)
        layer(ctx, w, h, 0x9A1010, SupernovaClientState.red);
        // Oscurecimiento posterior
        layer(ctx, w, h, 0x070307, SupernovaClientState.dark);

        switch (ph) {
            case P1_EXPLOSION -> {
                float a = t < 3 ? t / 3f * 0.85f : 0.85f * (float) Math.exp(-(t - 3) / 12f);
                layer(ctx, w, h, 0xFFF4E8, a);
            }
            case P2_COUNTDOWN -> {
                // El blanco solo aparece en el tramo final del colapso
                int dur = SupernovaClientState.duration;
                float f = (t - (dur - 100)) / 100f;
                if (f > 0) layer(ctx, w, h, 0xFFFFFF, Math.min(0.7f, f * f * f * 0.7f));
            }
            case P2_EXPLOSION -> {
                float flash = t < 4 ? t / 4f * 0.95f : 0.95f * (float) Math.exp(-(t - 4) / 18f);
                layer(ctx, w, h, 0xFFFFFF, flash);
                // pérdida de contraste con recuperación progresiva
                layer(ctx, w, h, 0xB4B4C0, 0.35f * (float) Math.exp(-t / 60f));
                beam(ctx, w, h, t);
            }
            default -> { }
        }

        if (ph == SupernovaPhase.P1_COUNTDOWN || ph == SupernovaPhase.P2_COUNTDOWN) {
            int secs = (int) Math.ceil(SupernovaClientState.remainingTicks() / 20.0);
            String label = ph == SupernovaPhase.P1_COUNTDOWN ? "COLAPSO EN " : "EXPLOSIÓN EN ";
            String text = label + (secs / 60) + ":" + String.format("%02d", secs % 60);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(w / 2f, 22f, 0f);
            ctx.getMatrices().scale(2f, 2f, 1f);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(text), 0, 0,
                    secs <= 10 ? 0xFF4444 : 0xFFD0A0);
            ctx.getMatrices().pop();
        }
    }

    /** Rayo blanco que avanza de izquierda a derecha dejando una estela que se desvanece. */
    private static void beam(DrawContext ctx, int w, int h, float t) {
        float start = 8f, len = 60f;
        if (t < start || t > start + len + 25f) return;
        float head = Math.min(1f, (t - start) / len) * w;
        float fade = t > start + len ? 1f - (t - start - len) / 25f : 1f;
        int y = (int) (h * 0.40f);
        int seg = 8;
        for (int x = 0; x < head; x += seg) {
            float k = x / Math.max(1f, head); // 0 cola, 1 cabeza
            float a = k * k * fade;
            ctx.fill(x, y - 7, x + seg, y + 7, argb(0xBFD8FF, 0.25f * a));
            ctx.fill(x, y - 3, x + seg, y + 3, argb(0xFFFFFF, 0.70f * a));
            ctx.fill(x, y - 1, x + seg, y + 1, argb(0xFFFFFF, a));
        }
    }

    private static void layer(DrawContext ctx, int w, int h, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        ctx.fill(0, 0, w, h, argb(rgb, alpha));
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private SupernovaOverlay() {}
}
