package moe.mchdf.quake.client.settings;

import java.util.HashMap;
import java.util.Map;
import moe.mchdf.quake.network.PlayerSettingC2SPayload;
import moe.mchdf.quake.network.PlayerSettingsS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * 서버 DB에 저장되는 내 설정(사이드바, 자동 다시 참여 등). 바닐라 사용자와 같은 값을 쓰도록 모드는 값을
 * 저장하지 않고 서버가 보내 준 값을 보여 주며, 바꾸면 서버에 보냅니다. 클라이언트 스레드에서만 씁니다.
 *
 * <p>항목 이름과 값은 플러그인의 {@code moe.mchdf.quake.settings.PlayerSettings}와 같아야 합니다.
 */
public final class ServerSettings {
	public static final String SIDEBAR = "sidebar";
	public static final String AUTO_REJOIN = "auto_rejoin";

	private static final Map<String, String> values = new HashMap<>();

	private ServerSettings() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PlayerSettingsS2CPayload.TYPE, (payload, context) -> {
			values.clear();
			values.putAll(payload.values());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(values::clear));
	}

	/** 서버가 아직 값을 보내지 않았으면(접속 직후 등) {@code null} */
	public static Boolean bool(String key) {
		String value = values.get(key);
		return value == null ? null : Boolean.parseBoolean(value);
	}

	/** 켜기/끄기 항목을 반대로 바꿔 서버에 보낸다. 값을 아직 받지 못했으면 아무것도 하지 않는다. */
	public static void toggle(String key) {
		Boolean current = bool(key);
		if (current == null || !ClientPlayNetworking.canSend(PlayerSettingC2SPayload.TYPE)) {
			return;
		}
		String next = Boolean.toString(!current);
		// 서버 응답을 기다리지 않고 화면에 바로 보여 준다. 서버가 거절하면 다시 보내 준 값으로 돌아간다.
		values.put(key, next);
		ClientPlayNetworking.send(new PlayerSettingC2SPayload(key, next));
	}
}
