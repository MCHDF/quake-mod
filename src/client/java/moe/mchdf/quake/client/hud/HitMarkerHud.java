package moe.mchdf.quake.client.hud;

import moe.mchdf.quake.QuakeMod;
import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.settings.QuakeSettings;
import moe.mchdf.quake.client.settings.QuakeSettings.HitMarkerShape;
import moe.mchdf.quake.client.ui.QuakeTheme;
import moe.mchdf.quake.network.HitS2CPayload;
import moe.mchdf.quake.network.HitS2CPayload.Zone;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/**
 * 내가 쏜 레일건이 상대를 맞히면 조준점 둘레에 히트마커를 잠깐 그립니다.
 *
 * <p>맞았는지와 맞은 부위는 서버가 판정해 {@link HitS2CPayload}로 알려 주고, 이 클래스는 그리기만 합니다.
 * 한 방에 처치되는 게임이라 상태는 두 가지이며 색은 고정입니다: 처치(몸통) 노랑, 헤드샷 빨강.
 * 모양은 설정({@link QuakeSettings#hitMarkerShape()})에서 고릅니다(기획 시안 A~D). 다시 맞히면 처음부터 다시 그립니다.
 */
public final class HitMarkerHud {
	static final int KILL_COLOR = 0xFFFFC83D;
	static final int HEADSHOT_COLOR = 0xFFFF3B3B;
	private static final int SHADOW_COLOR = 0x99000000;

	/** 전체 표시 시간(10틱). 마지막 {@link #FADE_PORTION} 구간에서 사라집니다. */
	public static final long DURATION_NANOS = 500_000_000L;
	private static final float FADE_PORTION = 0.4f;

	private static Zone zone;
	private static long shownAtNanos;

	private HitMarkerHud() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(HitS2CPayload.TYPE,
				(payload, context) -> show(payload.zone()));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(HitMarkerHud::clear));
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, QuakeMod.id("hit_marker"),
				HitMarkerHud::extract);
	}

	private static void show(Zone hitZone) {
		zone = hitZone;
		shownAtNanos = System.nanoTime();
	}

	private static void clear() {
		zone = null;
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (zone == null || !QuakeServerConnection.isActive()) {
			return;
		}
		long elapsed = System.nanoTime() - shownAtNanos;
		if (elapsed >= DURATION_NANOS) {
			clear();
			return;
		}
		draw(graphics, PixelShape.centerX(graphics), PixelShape.centerY(graphics),
				QuakeSettings.get().hitMarkerShape(), zone, elapsed);
	}

	/** 중심 픽셀 ({@code cx}, {@code cy})에 맞은 뒤 {@code elapsed}만큼 지난 히트마커를 그립니다. 설정 미리보기도 씁니다. */
	public static void draw(GuiGraphicsExtractor graphics, int cx, int cy, HitMarkerShape shape, Zone hitZone, long elapsed) {
		float progress = Math.clamp((float) elapsed / DURATION_NANOS, 0f, 1f);
		float fade = progress < 1f - FADE_PORTION ? 0f : (progress - (1f - FADE_PORTION)) / FADE_PORTION;
		float alpha = 1f - fade;
		int color = QuakeTheme.fade(hitZone == Zone.HEAD ? HEADSHOT_COLOR : KILL_COLOR, alpha);
		int shadow = QuakeTheme.fade(SHADOW_COLOR, alpha);
		switch (shape) {
			case CLASSIC_X -> classicX(graphics, cx, cy, elapsed, fade, color, shadow);
			case PIXEL_X -> pixelX(progress).draw(graphics, cx, cy, color, shadow);
			case RING_PULSE -> ringPulse(progress, hitZone).draw(graphics, cx, cy, color, shadow);
			case BRACKETS -> brackets(progress).draw(graphics, cx, cy, color, shadow);
		}
	}

	/**
	 * A. 클래식 X: 대각선 4개. 맞으면 1.15배로 살짝 튀었다가 0.1초 동안 돌아오고, 사라지면서 바깥으로 밀려납니다.
	 */
	private static void classicX(GuiGraphicsExtractor graphics, int cx, int cy, long elapsed, float fade,
								 int color, int shadow) {
		long popNanos = 100_000_000L;
		float popScale = 1.15f;
		float pop = elapsed >= popNanos ? 1f : popScale - (popScale - 1f) * elapsed / popNanos;
		float gap = 3f + 2.5f * fade;
		int arm = 4;
		Matrix3x2fStack pose = graphics.pose();
		for (int i = 0; i < 4; i++) {
			pose.pushMatrix();
			// 가운데 픽셀의 중심에서 회전합니다.
			pose.translate(cx + 0.5f, cy + 0.5f);
			pose.rotate((float) (Math.PI / 4 + Math.PI / 2 * i));
			pose.scale(pop);
			pose.translate(gap, 0f);
			graphics.fill(-1, -1, arm + 1, 1, shadow);
			pose.translate(0f, -0.5f);
			graphics.fill(0, 0, arm, 1, color);
			pose.popMatrix();
		}
	}

	/** B. 픽셀 X: 한 픽셀씩 찍은 대각선. 안쪽에서 바깥쪽으로 한 칸씩 켜집니다. */
	private static PixelShape pixelX(float progress) {
		int lit = Math.min(4, 1 + (int) (progress / 0.08f));
		PixelShape s = new PixelShape();
		for (int i = 0; i < lit; i++) {
			int d = 3 + i;
			s.add(d, d).add(-d, d).add(d, -d).add(-d, -d);
		}
		return s;
	}

	/** C. 링 펄스: 원이 퍼지며 사라집니다. 헤드샷은 가운데 점을 더합니다. 시선을 가장 덜 가립니다. */
	private static PixelShape ringPulse(float progress, Zone hitZone) {
		float eased = 1f - (1f - progress) * (1f - progress);
		PixelShape s = new PixelShape().circle(4f + 5f * eased);
		if (hitZone == Zone.HEAD) {
			s.add(0, 0);
		}
		return s;
	}

	/** D. 코너 꺾쇠: 네 귀퉁이 꺾쇠가 바깥에서 안쪽으로 조여 듭니다. */
	private static PixelShape brackets(float progress) {
		float squeeze = Math.min(1f, progress / 0.25f);
		int d = Math.round(9f - 3f * squeeze);
		PixelShape s = new PixelShape();
		for (int sx : new int[] {-1, 1}) {
			for (int sy : new int[] {-1, 1}) {
				s.horizontal(sx * d, sx * (d - 2), sy * d).vertical(sx * d, sy * d, sy * (d - 2));
			}
		}
		return s;
	}
}
