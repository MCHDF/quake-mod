package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 클라 → 서버: 모드가 설치되어 있음을 알리고 모드의 프로토콜 버전을 보냅니다.
 * 형식: int(빅엔디언) 프로토콜 버전.
 */
public record HandshakeC2SPayload(int protocolVersion) implements CustomPacketPayload {
	public static final Type<HandshakeC2SPayload> TYPE = new Type<>(QuakeProtocol.HANDSHAKE);
	public static final StreamCodec<ByteBuf, HandshakeC2SPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, HandshakeC2SPayload::protocolVersion,
			HandshakeC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
