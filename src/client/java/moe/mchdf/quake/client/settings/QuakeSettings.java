package moe.mchdf.quake.client.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import moe.mchdf.quake.QuakeMod;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 모드 사용자 전용 설정입니다. 화면에 그리는 방식만 바꾸므로 서버에 보내지 않고 클라이언트의
 * {@code config/quake.json}에 저장합니다. 값을 바꾸면 바로 저장합니다.
 */
public final class QuakeSettings {
	/** 레일건 조준점 모양 (기획 시안 1~5) */
	public enum CrosshairShape implements Labeled {
		CLASSIC_CROSS("클래식 크로스"),
		CENTER_DOT("센터 도트"),
		CIRCLE_DOT("서클 + 도트"),
		TEE("T자 크로스"),
		BRACKETS("코너 브래킷");

		private final String label;

		CrosshairShape(String label) {
			this.label = label;
		}

		@Override
		public String label() {
			return label;
		}
	}

	/** 레일건 조준점 색 (ARGB) */
	public enum CrosshairColor implements Labeled {
		WHITE("흰색", 0xFFFFFFFF),
		GREEN("초록", 0xFF5BFF6A),
		RED("빨강", 0xFFFF4D4D),
		CYAN("청록", 0xFF4DF0FF);

		private final String label;
		private final int argb;

		CrosshairColor(String label, int argb) {
			this.label = label;
			this.argb = argb;
		}

		@Override
		public String label() {
			return label;
		}

		public int argb() {
			return argb;
		}
	}

	/** 히트마커 모양 (기획 시안 A~D). 색은 처치 노랑, 헤드샷 빨강으로 고정입니다. */
	public enum HitMarkerShape implements Labeled {
		CLASSIC_X("클래식 X"),
		PIXEL_X("픽셀 X"),
		RING_PULSE("링 펄스"),
		BRACKETS("코너 꺾쇠");

		private final String label;

		HitMarkerShape(String label) {
			this.label = label;
		}

		@Override
		public String label() {
			return label;
		}
	}

	/** 설정 화면에 보여 줄 이름이 있는 값 */
	public interface Labeled {
		String label();
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("quake.json");
	private static QuakeSettings instance;

	// JSON으로 저장되는 값. 파일에 없거나 잘못된 값이면 load()에서 기본값으로 채웁니다.
	private CrosshairShape crosshairShape = CrosshairShape.CLASSIC_CROSS;
	private CrosshairColor crosshairColor = CrosshairColor.WHITE;
	private HitMarkerShape hitMarkerShape = HitMarkerShape.CLASSIC_X;
	private boolean cooldownRing = true;

	public static QuakeSettings get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public CrosshairShape crosshairShape() {
		return crosshairShape;
	}

	public CrosshairColor crosshairColor() {
		return crosshairColor;
	}

	public HitMarkerShape hitMarkerShape() {
		return hitMarkerShape;
	}

	public boolean cooldownRing() {
		return cooldownRing;
	}

	public void setCrosshairShape(CrosshairShape value) {
		crosshairShape = value;
		save();
	}

	public void setCrosshairColor(CrosshairColor value) {
		crosshairColor = value;
		save();
	}

	public void setHitMarkerShape(HitMarkerShape value) {
		hitMarkerShape = value;
		save();
	}

	public void setCooldownRing(boolean value) {
		cooldownRing = value;
		save();
	}

	/** 목록의 다음 값. 마지막 다음은 처음입니다. */
	public static <E extends Enum<E>> E next(E value) {
		E[] values = value.getDeclaringClass().getEnumConstants();
		return values[(value.ordinal() + 1) % values.length];
	}

	/** 목록의 이전 값. 처음 이전은 마지막입니다. */
	public static <E extends Enum<E>> E previous(E value) {
		E[] values = value.getDeclaringClass().getEnumConstants();
		return values[(value.ordinal() + values.length - 1) % values.length];
	}

	private static QuakeSettings load() {
		QuakeSettings settings = null;
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				settings = GSON.fromJson(reader, QuakeSettings.class);
			} catch (IOException | JsonParseException e) {
				QuakeMod.LOGGER.warn("Quake 설정 파일을 읽지 못해 기본값을 씁니다: {}", e.getMessage());
			}
		}
		if (settings == null) {
			settings = new QuakeSettings();
		}
		// 파일에 없는 항목이나 알 수 없는 값은 Gson이 null로 두므로 기본값으로 되돌립니다.
		QuakeSettings defaults = new QuakeSettings();
		if (settings.crosshairShape == null) {
			settings.crosshairShape = defaults.crosshairShape;
		}
		if (settings.crosshairColor == null) {
			settings.crosshairColor = defaults.crosshairColor;
		}
		if (settings.hitMarkerShape == null) {
			settings.hitMarkerShape = defaults.hitMarkerShape;
		}
		return settings;
	}

	private void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			QuakeMod.LOGGER.warn("Quake 설정을 저장하지 못했습니다: {}", e.getMessage());
		}
	}
}
