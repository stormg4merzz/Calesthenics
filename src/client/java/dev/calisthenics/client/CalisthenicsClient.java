package dev.calisthenics.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.calisthenics.Calisthenics;
import dev.calisthenics.Skill;
import dev.calisthenics.network.SkillBroadcastS2C;
import dev.calisthenics.network.SkillRequestC2S;
// NOTE: the Player Animation Library (PAL) imports below come from its docs/repo layout and could not be
// compiled here. If your IDE marks one red, use "auto import" (Alt+Enter in IntelliJ) to pick the right package.
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Client side: key + emote UI, sends requests to the server, and plays the animation on whichever
 * player the server says is doing an emote (so you see everyone, and they see you).
 */
public class CalisthenicsClient implements ClientModInitializer {

    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(Calisthenics.MOD_ID, "emotes");
    private static final int FADE_TICKS = 6;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Calisthenics.MOD_ID, "main"));

    public static KeyMapping menuKey;
    private static KeyMapping stopKey;

    /** The held emote this client's own player is doing ("" = none). */
    private static String mySkill = "";

    @Override
    public void onInitializeClient() {
        // One animation layer per player model. The "STOP" state means "play nothing unless we trigger an emote".
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER_ID, 1000,
                player -> new PlayerAnimationController(player, (controller, state, animSetter) -> PlayState.STOP));

        ClientPlayNetworking.registerGlobalReceiver(SkillBroadcastS2C.TYPE,
                (payload, context) -> applyTo(payload.player(), payload.skill()));

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.calisthenics.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY));
        stopKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.calisthenics.stop", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(CalisthenicsClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> mySkill = "");
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        while (menuKey.consumeClick()) {
            client.setScreen(new EmoteWheelScreen());
        }
        while (stopKey.consumeClick()) {
            requestSkill("");
        }

        // Moving, jumping or sneaking ends a held emote, like any other emote.
        if (!mySkill.isEmpty() && client.screen == null) {
            Options o = client.options;
            if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown()
                    || o.keyJump.isDown() || o.keyShift.isDown()) {
                requestSkill("");
            }
        }
    }

    /** Ask the server to start (or with "" stop) an emote for the local player. */
    public static void requestSkill(String skillId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (!ClientPlayNetworking.canSend(SkillRequestC2S.TYPE)) {
            client.player.displayClientMessage(Component.translatable("message.calisthenics.no_server"), true);
            return;
        }
        // Only held emotes count as "active"; one-shot flips end by themselves.
        Skill parsed = Skill.byId(skillId);
        mySkill = (parsed != null && !parsed.oneShot) ? skillId : "";
        ClientPlayNetworking.send(new SkillRequestC2S(skillId));
    }

    private static void applyTo(UUID playerId, String skillId) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Player entity = client.level.getPlayerByUUID(playerId);
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        PlayerAnimationController controller =
                (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
        if (controller == null) {
            return;
        }

        if (skillId.isEmpty()) {
            controller.stop();
            return;
        }

        // The animation id is "<modid>:<animation name inside the json>", which we named after the emote id.
        controller.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(FADE_TICKS, EasingType.EASE_IN_OUT_SINE),
                Identifier.fromNamespaceAndPath(Calisthenics.MOD_ID, skillId));
    }
}
