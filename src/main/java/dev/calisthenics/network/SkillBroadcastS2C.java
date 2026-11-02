package dev.calisthenics.network;

import dev.calisthenics.Calisthenics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Server -> clients: "this player is now doing this emote" (empty string = stopped). */
public record SkillBroadcastS2C(UUID player, String skill) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillBroadcastS2C> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Calisthenics.MOD_ID, "skill_broadcast"));

    public static final StreamCodec<ByteBuf, SkillBroadcastS2C> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkillBroadcastS2C::player,
            ByteBufCodecs.STRING_UTF8, SkillBroadcastS2C::skill,
            SkillBroadcastS2C::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
