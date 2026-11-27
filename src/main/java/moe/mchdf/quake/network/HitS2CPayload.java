package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 서버 → 클라: 내가 쏜 레일건이 상대를 맞혔고, 어느 부위였는지 알려 줍니다.
 *
 * <p>형식: byte 맞은 부위(0 몸통, 1 머리). 판정은 서버가 하므로 모드는 이 값으로 히트마커만 그립니다.
 * 플러그인의 {@code moe.mchdf.quake.mod.ModHitMarker#encode}와 형식이 같아야 합니다.
 */
public record HitS2CPayload(Zone zone) implements CustomPacketPayload {
	/** 맞은 부위 */
	public enum Zone {
		BODY,
		HEAD
	}

	private static final int ZONE_BODY = 0;
	private static final int ZONE_HEAD = 1;

	public static final Type<HitS2CPayload> TYPE = new Type<>(QuakeProtocol.HIT);
	public static final StreamCodec<ByteBuf, HitS2CPayload> CODEC = StreamCodec.of(
			(buf, payload) -> buf.writeByte(payload.zone() == Zone.HEAD ? ZONE_HEAD : ZONE_BODY),
			buf -> new HitS2CPayload(readZone(buf)));

	private static Zone readZone(ByteBuf buf) {
		int zone = buf.readByte();
		return switch (zone) {
			case ZONE_BODY -> Zone.BODY;
			case ZONE_HEAD -> Zone.HEAD;
			default -> throw new DecoderException("알 수 없는 맞은 부위: " + zone);
		};
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
