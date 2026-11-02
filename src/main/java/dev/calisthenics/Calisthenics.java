package dev.calisthenics;

import dev.calisthenics.network.SkillBroadcastS2C;
import dev.calisthenics.network.SkillRequestC2S;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side: validates emote requests and relays them to every player who can see the sender.
 * Also remembers who is mid-emote so players who walk into range later still see it.
 */
public class Calisthenics implements ModInitializer {

    public static final String MOD_ID = "calisthenics";

    /** Minimum ticks between two requests from one player (anti-spam). */
    private static final int COOLDOWN_TICKS = 4;

    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(SkillRequestC2S.TYPE, SkillRequestC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillBroadcastS2C.TYPE, SkillBroadcastS2C.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SkillRequestC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String skill = payload.skill();

            // Only known emotes (or "" = stop) are accepted; never trust the client string.
            if (!skill.isEmpty() && Skill.byId(skill) == null) {
                return;
            }

            int now = context.server().getTickCount();
            Integer last = LAST_REQUEST_TICK.get(player.getUUID());
            if (last != null && now - last < COOLDOWN_TICKS) {
                return;
            }
            LAST_REQUEST_TICK.put(player.getUUID(), now);

            // Held emotes are remembered (so late joiners see them); stop requests and one-shot flips are not.
            Skill parsed = Skill.byId(skill);
            if (parsed == null || parsed.oneShot) {
                ACTIVE.remove(player.getUUID());
            } else {
                ACTIVE.put(player.getUUID(), skill);
            }

            SkillBroadcastS2C message = new SkillBroadcastS2C(player.getUUID(), skill);
            sendIfPossible(player, message);
            for (ServerPlayer other : PlayerLookup.tracking(player)) {
                sendIfPossible(other, message);
            }
        });

        // Someone walks into range of a player who is already doing an emote -> show it to them.
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, watcher) -> {
            if (trackedEntity instanceof ServerPlayer target) {
                String skill = ACTIVE.get(target.getUUID());
                if (skill != null) {
                    sendIfPossible(watcher, new SkillBroadcastS2C(target.getUUID(), skill));
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            ACTIVE.remove(id);
            LAST_REQUEST_TICK.remove(id);
        });
    }

    private static void sendIfPossible(ServerPlayer to, SkillBroadcastS2C message) {
        // Clients without the mod simply never get the packet.
        if (ServerPlayNetworking.canSend(to, SkillBroadcastS2C.TYPE)) {
            ServerPlayNetworking.send(to, message);
        }
    }
}
