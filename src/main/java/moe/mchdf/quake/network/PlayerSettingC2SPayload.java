package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 클라 → 서버: 설정 화면에서 바꾼 서버 설정 항목 하나입니다.
 *
 * <p>형식: 문자열 이름, 문자열 값(VarInt 길이 + UTF-8). 서버가 값을 검사해 DB에 저장한 뒤
 * {@link PlayerSettingsS2CPayload}로 전체를 다시 보내 줍니다. {@link PlayerSettingsS2CPayload}와 같은 채널을 씁니다.
 */
public record PlayerSettingC2SPayload(String key, String value) implements CustomPacketPayload {
	public static final Type<PlayerSettingC2SPayload> TYPE = new Type<>(QuakeProtocol.PLAYER_SETTINGS);
	public static final StreamCodec<ByteBuf, PlayerSettingC2SPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PlayerSettingC2SPayload::key,
			ByteBufCodecs.STRING_UTF8, PlayerSettingC2SPayload::value,
			PlayerSettingC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
