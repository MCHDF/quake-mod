package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 서버 → 클라: 서버 DB에 저장된 내 설정 전체(항목 이름 → 값)입니다. 사이드바, 자동 다시 참여처럼
 * 서버가 적용하는 설정이라 값은 서버가 들고 있고, 모드는 설정 화면에 보여 주기만 합니다.
 *
 * <p>형식: VarInt 항목 수, 항목마다 문자열 이름과 문자열 값(VarInt 길이 + UTF-8).
 * 접속 직후와 설정이 바뀔 때마다 옵니다. 플러그인의 {@code moe.mchdf.quake.mod.ModPlayerSettings#encode}와
 * 형식이 같아야 합니다. 모르는 항목은 무시하면 되므로 항목이 늘어도 형식은 그대로입니다.
 */
public record PlayerSettingsS2CPayload(Map<String, String> values) implements CustomPacketPayload {
	/** 서버가 보내는 항목 수의 상한입니다. */
	public static final int MAX_ENTRIES = 64;

	public static final Type<PlayerSettingsS2CPayload> TYPE = new Type<>(QuakeProtocol.PLAYER_SETTINGS);
	public static final StreamCodec<ByteBuf, PlayerSettingsS2CPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8, MAX_ENTRIES),
			PlayerSettingsS2CPayload::values,
			PlayerSettingsS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
