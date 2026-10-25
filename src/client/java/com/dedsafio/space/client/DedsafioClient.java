package com.dedsafio.space.client;

import com.dedsafio.space.net.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class DedsafioClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SUPERNOVA_SYNC,
                (client, handler, buf, responseSender) -> SupernovaClientState.applyFromBuf(buf, client));
        ClientTickEvents.END_CLIENT_TICK.register(c -> SupernovaClientState.tick());
        HudRenderCallback.EVENT.register(SupernovaOverlay::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SupernovaClientState.reset());
    }
}
