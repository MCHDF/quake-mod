package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 서버 → 클라: 모드 전용 설정 화면을 엽니다. 내용은 없습니다.
 * 플러그인의 {@code moe.mchdf.quake.mod.ModSettingsScreen}이 보냅니다.
 */
public record OpenSettingsS2CPayload() implements CustomPacketPayload {
	public static final OpenSettingsS2CPayload INSTANCE = new OpenSettingsS2CPayload();
	public static final Type<OpenSettingsS2CPayload> TYPE = new Type<>(QuakeProtocol.OPEN_SETTINGS);
	public static final StreamCodec<ByteBuf, OpenSettingsS2CPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
