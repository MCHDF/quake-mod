package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 서버 → 클라: 핸드셰이크 응답입니다.
 * 형식: int(빅엔디언) 서버 프로토콜 버전, boolean(1바이트) 수락 여부.
 */
public record HandshakeS2CPayload(int protocolVersion, boolean accepted) implements CustomPacketPayload {
	public static final Type<HandshakeS2CPayload> TYPE = new Type<>(QuakeProtocol.HANDSHAKE);
	public static final StreamCodec<ByteBuf, HandshakeS2CPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, HandshakeS2CPayload::protocolVersion,
			ByteBufCodecs.BOOL, HandshakeS2CPayload::accepted,
			HandshakeS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
