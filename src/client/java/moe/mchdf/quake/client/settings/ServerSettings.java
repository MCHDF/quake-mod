package moe.mchdf.quake.client.settings;

import java.util.HashMap;
import java.util.List;
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

	/** 효과음 종류. 화면에 이 순서로 보여 줍니다. */
	public enum SoundType {
		HIT_CONFIRM("sound.hit", "적중음"),
		KILL("sound.kill", "처치음"),
		OWN_SHOT("sound.own_shot", "내 발사음"),
		OTHER_SHOT("sound.other_shot", "다른 사람 발사음"),
		COUNTDOWN("sound.countdown", "카운트다운");

		private final String key;
		private final String label;

		SoundType(String key, String label) {
			this.key = key;
			this.label = label;
		}

		public String key() {
			return key;
		}

		public String label() {
			return label;
		}
	}

	/** 효과음 음량 단계의 값과 글자. 서버가 이 순서로 단계를 오르내립니다. */
	private static final List<String> SOUND_LEVELS = List.of("off", "low", "medium", "high");
	private static final List<String> SOUND_LABELS = List.of("끄기", "작게", "보통", "크게");

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

	/** 효과음 음량 글자. 서버 값을 아직 받지 못했거나 모르는 값이면 {@code null} */
	public static String soundLabel(SoundType type) {
		int index = SOUND_LEVELS.indexOf(values.get(type.key()));
		return index < 0 ? null : SOUND_LABELS.get(index);
	}

	/**
	 * 효과음 음량을 한 단계 올리거나(끄기 다음은 작게, 크게 다음은 끄기) 내려 서버에 보낸다.
	 * 서버가 저장한 뒤 바뀐 음량으로 그 소리를 들려준다.
	 */
	public static void cycleSound(SoundType type, boolean forward) {
		int index = SOUND_LEVELS.indexOf(values.get(type.key()));
		if (index < 0) {
			return;
		}
		int size = SOUND_LEVELS.size();
		send(type.key(), SOUND_LEVELS.get((index + (forward ? 1 : size - 1)) % size));
	}

	/** 켜기/끄기 항목을 반대로 바꿔 서버에 보낸다. 값을 아직 받지 못했으면 아무것도 하지 않는다. */
	public static void toggle(String key) {
		Boolean current = bool(key);
		if (current != null) {
			send(key, Boolean.toString(!current));
		}
	}

	private static void send(String key, String value) {
		if (!ClientPlayNetworking.canSend(PlayerSettingC2SPayload.TYPE)) {
			return;
		}
		// 서버 응답을 기다리지 않고 화면에 바로 보여 준다. 서버가 거절하면 다시 보내 준 값으로 돌아간다.
		values.put(key, value);
		ClientPlayNetworking.send(new PlayerSettingC2SPayload(key, value));
	}
}
