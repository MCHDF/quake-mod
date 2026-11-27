package moe.mchdf.quake.client.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.settings.QuakeSettings;
import moe.mchdf.quake.client.settings.QuakeSettings.CrosshairShape;
import moe.mchdf.quake.client.ui.QuakeTheme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 레일건을 들고 있을 때 바닐라 조준점 대신 설정한 모양·색의 조준점을 그리고, 쿨다운 중에는 둘레에 링을 그립니다.
 *
 * <p>레일건이 아니거나 3인칭이면 바닐라 조준점을 그대로 그립니다. 레일건은 플러그인이 아이템에 기록한 id
 * ({@code quake:item_id})로 알아봅니다. 쿨다운은 서버가 아이템 쿨다운으로 걸기 때문에 클라이언트가 이미 알고 있어
 * 따로 패킷을 받지 않습니다.
 */
public final class RailgunCrosshairHud {
	/** 플러그인이 Bukkit PDC로 기록한 아이템 id는 custom_data의 이 묶음 안에 들어 있습니다. */
	private static final String BUKKIT_VALUES = "PublicBukkitValues";
	private static final String ITEM_ID = "quake:item_id";
	private static final Set<String> RAILGUN_IDS = Set.of("railgun", "railgun_test");

	private static final int OUTLINE = 0xB0000000;
	/** 쿨다운 링 반지름(GUI 픽셀). 히트마커(가운데서 최대 약 9픽셀)보다 바깥에 둡니다. */
	private static final int RING_RADIUS = 11;
	private static final int RING_TRACK = 0x40FFFFFF;
	private static final int RING_OUTLINE = 0x50000000;
	/** 준비 완료 순간 링이 밝게 남았다 사라지는 시간 */
	private static final long READY_FLASH_NANOS = 200_000_000L;

	private static final Map<CrosshairShape, PixelShape> SHAPES = new EnumMap<>(CrosshairShape.class);
	/** 링 픽셀을 12시 방향부터 시계 방향 순서로. 링 바탕과 차오르는 부분이 같은 픽셀을 쓰도록 여기서 만듭니다. */
	private static final List<int[]> RING_ORDER = ringOrder();
	private static final PixelShape RING = ringShape();

	private static boolean wasCoolingDown;
	private static long readyAtNanos;

	private RailgunCrosshairHud() {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (graphics, deltaTracker) ->
				extract(graphics, deltaTracker, vanilla));
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, HudElement vanilla) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || !QuakeServerConnection.isActive() || !client.options.getCameraType().isFirstPerson()
				|| !isRailgun(player.getMainHandItem())) {
			wasCoolingDown = false;
			vanilla.extractRenderState(graphics, deltaTracker);
			return;
		}
		QuakeSettings settings = QuakeSettings.get();
		int cx = PixelShape.centerX(graphics);
		int cy = PixelShape.centerY(graphics);
		int color = settings.crosshairColor().argb();
		shape(settings.crosshairShape()).draw(graphics, cx, cy, color, OUTLINE);
		if (settings.cooldownRing()) {
			float cooldown = player.getCooldowns().getCooldownPercent(player.getMainHandItem(),
					deltaTracker.getGameTimeDeltaPartialTick(false));
			drawCooldownRing(graphics, cx, cy, cooldown, color);
		}
	}

	/** 설정 화면 미리보기용: 지정한 모양·색으로 조준점을 그립니다. */
	public static void drawPreview(GuiGraphicsExtractor graphics, int cx, int cy, CrosshairShape shape, int color,
							float cooldownProgress) {
		shape(shape).draw(graphics, cx, cy, color, OUTLINE);
		if (cooldownProgress >= 0f) {
			RING.draw(graphics, cx, cy, RING_TRACK, RING_OUTLINE);
			fillRing(graphics, cx, cy, cooldownProgress, color);
		}
	}

	/**
	 * 쿨다운이 생기면 링이 나타나 12시 방향부터 시계 방향으로 차오르고, 다 차면 잠깐 밝게 남았다가 사라집니다.
	 * {@code cooldown}은 남은 비율(1 → 0)입니다.
	 */
	private static void drawCooldownRing(GuiGraphicsExtractor graphics, int cx, int cy, float cooldown, int color) {
		long now = System.nanoTime();
		if (cooldown > 0f) {
			wasCoolingDown = true;
			RING.draw(graphics, cx, cy, RING_TRACK, RING_OUTLINE);
			fillRing(graphics, cx, cy, 1f - cooldown, color);
			return;
		}
		if (wasCoolingDown) {
			wasCoolingDown = false;
			readyAtNanos = now;
		}
		long sinceReady = now - readyAtNanos;
		if (readyAtNanos != 0 && sinceReady < READY_FLASH_NANOS) {
			float alpha = 1f - (float) sinceReady / READY_FLASH_NANOS;
			RING.draw(graphics, cx, cy, QuakeTheme.fade(color, alpha), QuakeTheme.fade(RING_OUTLINE, alpha));
		}
	}

	private static void fillRing(GuiGraphicsExtractor graphics, int cx, int cy, float progress, int color) {
		int filled = Math.round(RING_ORDER.size() * Math.clamp(progress, 0f, 1f));
		for (int i = 0; i < filled; i++) {
			int[] p = RING_ORDER.get(i);
			graphics.fill(cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, color);
		}
	}

	static boolean isRailgun(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return false;
		}
		String id = data.copyTag().getCompoundOrEmpty(BUKKIT_VALUES).getStringOr(ITEM_ID, "");
		return RAILGUN_IDS.contains(id);
	}

	private static PixelShape shape(CrosshairShape shape) {
		return SHAPES.computeIfAbsent(shape, RailgunCrosshairHud::build);
	}

	/** 기획 시안 1~5. 가운데 픽셀을 (0, 0)으로 두고 위아래·좌우 대칭으로 찍습니다. */
	private static PixelShape build(CrosshairShape shape) {
		PixelShape s = new PixelShape();
		switch (shape) {
			case CLASSIC_CROSS -> {
				// 가운데가 빈 십자: 가운데서 3~6픽셀
				s.horizontal(3, 6, 0).horizontal(-6, -3, 0).vertical(0, 3, 6).vertical(0, -6, -3);
			}
			case CENTER_DOT -> s.add(0, 0);
			case CIRCLE_DOT -> s.circle(5).add(0, 0);
			case TEE -> {
				// 위쪽 선이 없는 십자 + 가운데 점
				s.horizontal(3, 6, 0).horizontal(-6, -3, 0).vertical(0, 3, 6).add(0, 0);
			}
			case BRACKETS -> {
				// 네 귀퉁이 꺾쇠 + 가운데 점
				for (int sx : new int[] {-1, 1}) {
					for (int sy : new int[] {-1, 1}) {
						s.horizontal(sx * 5, sx * 3, sy * 5).vertical(sx * 5, sy * 5, sy * 3);
					}
				}
				s.add(0, 0);
			}
		}
		return s;
	}

	private static PixelShape ringShape() {
		PixelShape ring = new PixelShape();
		for (int[] p : RING_ORDER) {
			ring.add(p[0], p[1]);
		}
		return ring;
	}

	private static List<int[]> ringOrder() {
		Set<String> seen = new LinkedHashSet<>();
		List<int[]> points = new ArrayList<>();
		int steps = RING_RADIUS * 16;
		for (int i = 0; i < steps; i++) {
			double angle = Math.PI * 2 * i / steps;
			int x = (int) Math.round(Math.cos(angle) * RING_RADIUS);
			int y = (int) Math.round(Math.sin(angle) * RING_RADIUS);
			if (seen.add(x + "," + y)) {
				points.add(new int[] {x, y});
			}
		}
		// 화면 y는 아래로 갈수록 크므로, 12시(위)를 0으로 두고 시계 방향 각도로 정렬합니다.
		points.sort(Comparator.comparingDouble(p -> {
			double a = Math.atan2(p[0], -p[1]);
			return a < 0 ? a + Math.PI * 2 : a;
		}));
		return points;
	}
}
