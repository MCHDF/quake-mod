package moe.mchdf.quake.client.hud;

import moe.mchdf.quake.QuakeMod;
import moe.mchdf.quake.client.QuakeServerConnection;
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
 * 내가 쏜 레일건이 상대를 맞히면 조준점 둘레에 X자 히트마커를 잠깐 그립니다.
 *
 * <p>맞았는지와 맞은 부위는 서버가 판정해 {@link HitS2CPayload}로 알려 주고, 이 클래스는 그리기만 합니다.
 * 한 방에 처치되는 게임이라 상태는 두 가지이며 색은 고정입니다.
 * <ul>
 *   <li>처치(몸통): 노랑</li>
 *   <li>헤드샷: 빨강</li>
 * </ul>
 * 모양은 기획 시안 A(클래식 X)입니다. 맞으면 살짝 크게 튀었다가 제자리로 돌아오고,
 * 끝무렵에 선이 바깥으로 밀려나며 사라집니다. 다시 맞히면 처음부터 다시 그립니다.
 */
public final class HitMarkerHud {
	static final int KILL_COLOR = 0xFFFFC83D;
	static final int HEADSHOT_COLOR = 0xFFFF3B3B;
	private static final int SHADOW_COLOR = 0x99000000;

	/** 전체 표시 시간(10틱). 마지막 {@link #FADE_PORTION} 구간에서 사라집니다. */
	private static final long DURATION_NANOS = 500_000_000L;
	private static final float FADE_PORTION = 0.4f;
	/** 맞은 직후 커졌다가 돌아오는 시간과 처음 배율 */
	private static final long POP_NANOS = 100_000_000L;
	private static final float POP_SCALE = 1.15f;

	/** 조준점 중심에서 선이 시작하는 거리와 선 길이 (GUI 픽셀). 사라지면서 거리가 {@link #GAP_DRIFT}만큼 늘어납니다. */
	private static final float GAP = 3f;
	private static final float GAP_DRIFT = 2.5f;
	private static final int ARM_LENGTH = 4;

	/** 바닐라 조준점 그림 크기. 화면 가운데에 이 크기로 그려지므로 같은 중심을 씁니다. */
	private static final int CROSSHAIR_SIZE = 15;

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
		float progress = (float) elapsed / DURATION_NANOS;
		float fade = progress < 1f - FADE_PORTION ? 0f : (progress - (1f - FADE_PORTION)) / FADE_PORTION;
		float alpha = 1f - fade;
		float pop = elapsed >= POP_NANOS ? 1f : POP_SCALE - (POP_SCALE - 1f) * elapsed / POP_NANOS;
		float gap = GAP + GAP_DRIFT * fade;
		int color = QuakeTheme.fade(zone == Zone.HEAD ? HEADSHOT_COLOR : KILL_COLOR, alpha);
		int shadow = QuakeTheme.fade(SHADOW_COLOR, alpha);

		float centerX = (graphics.guiWidth() - CROSSHAIR_SIZE) / 2 + CROSSHAIR_SIZE / 2f;
		float centerY = (graphics.guiHeight() - CROSSHAIR_SIZE) / 2 + CROSSHAIR_SIZE / 2f;
		Matrix3x2fStack pose = graphics.pose();
		for (int i = 0; i < 4; i++) {
			pose.pushMatrix();
			pose.translate(centerX, centerY);
			pose.rotate((float) (Math.PI / 4 + Math.PI / 2 * i));
			pose.scale(pop);
			pose.translate(gap, 0f);
			// 밝은 배경(눈, 하늘)에서도 보이도록 한 픽셀 두꺼운 그림자를 먼저 깔고, 가운데에 1픽셀 선을 그립니다.
			graphics.fill(-1, -1, ARM_LENGTH + 1, 1, shadow);
			pose.translate(0f, -0.5f);
			graphics.fill(0, 0, ARM_LENGTH, 1, color);
			pose.popMatrix();
		}
	}
}
