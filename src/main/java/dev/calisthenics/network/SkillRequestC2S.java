package dev.calisthenics.network;

import dev.calisthenics.Calisthenics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: "I want to start this emote" (empty string = stop). */
public record SkillRequestC2S(String skill) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillRequestC2S> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Calisthenics.MOD_ID, "skill_request"));

    public static final StreamCodec<ByteBuf, SkillRequestC2S> CODEC =
            ByteBufCodecs.STRING_UTF8.map(SkillRequestC2S::new, SkillRequestC2S::skill);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
