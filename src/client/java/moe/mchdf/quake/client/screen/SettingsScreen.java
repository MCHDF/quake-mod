package moe.mchdf.quake.client.screen;

import java.util.function.Supplier;
import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.QuickSettingsKey;
import moe.mchdf.quake.client.hud.HitMarkerHud;
import moe.mchdf.quake.client.hud.RailgunCrosshairHud;
import moe.mchdf.quake.client.settings.QuakeSettings;
import moe.mchdf.quake.client.settings.QuakeSettings.CrosshairColor;
import moe.mchdf.quake.client.settings.ServerSettings;
import moe.mchdf.quake.client.ui.ArrowButton;
import moe.mchdf.quake.client.ui.ColorSwatchButton;
import moe.mchdf.quake.client.ui.PopupButton;
import moe.mchdf.quake.client.ui.QuakeTheme;
import moe.mchdf.quake.network.HitS2CPayload.Zone;
import moe.mchdf.quake.network.OpenSettingsS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * 빠른 설정. 게임 화면을 가리지 않도록 왼쪽에 패널만 띄우고, 바꾼 값은 화면 가운데 조준점 자리에 바로 그려 보여 줍니다.
 *
 * <p>여는 방법: 단축키(기본 F8, {@link QuickSettingsKey}), 설정 아이템 우클릭과 {@code /settings}
 * (서버가 {@link OpenSettingsS2CPayload}를 보냄). 닫기: ESC 또는 같은 단축키.
 *
 * <p>항목은 지금 동작하는 설정만 둡니다. 다른 사람 빔·감도·효과음처럼 기능이 생기는 항목은 그때 줄을 추가합니다.
 * 위쪽 모드 설정(조준점 등)은 바꾸는 즉시 {@link QuakeSettings}에, 구분선 아래 사이드바·자동 다시 참여는
 * 바닐라 사용자와 같이 서버 DB에 저장됩니다({@link ServerSettings}). 효과음은 「효과음 설정」 버튼으로 여는
 * 분류 화면({@link SoundSettingsScreen})에 따로 둡니다.
 */
public class SettingsScreen extends Screen {
	private static final int MARGIN = 12;
	private static final int PANEL_WIDTH = 200;
	private static final int PAD = 10;
	private static final int HEADER_HEIGHT = 22;
	private static final int ROW = 20;
	private static final int ARROW = 14;
	private static final int CONTROL_WIDTH = 112;
	private static final int SWATCH = 13;
	private static final int SWATCH_GAP = 4;
	private static final int BUTTON_GAP = 4;
	/** 모드(이 컴퓨터)에 저장하는 줄 수. 그 아래 줄은 서버에 저장합니다. */
	private static final int CLIENT_ROWS = 4;
	private static final int ROWS = 6;

	/** 가운데 미리보기: 쿨다운 1.8초가 차오른 뒤 잠깐 쉬기를 반복합니다. */
	private static final long COOLDOWN_LOOP_MS = 2600;
	private static final long COOLDOWN_MS = 1800;

	private final QuakeSettings settings = QuakeSettings.get();
	/** 적중 미리보기: 누를 때마다 처치와 헤드샷을 번갈아 보여 줍니다. */
	private Zone previewZone = Zone.HEAD;
	private long previewStartNanos;

	public SettingsScreen() {
		super(Component.literal("빠른 설정"));
	}

	/** 서버의 설정 열기 패킷을 받으면 화면을 엽니다. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(OpenSettingsS2CPayload.TYPE,
				(payload, context) -> open(context.client()));
	}

	/** 모드가 비활성(서버와 버전이 다름)이면 열지 않습니다. */
	public static void open(Minecraft client) {
		if (QuakeServerConnection.isActive()) {
			client.gui.setScreen(new SettingsScreen());
		}
	}

	private int panelLeft() {
		return MARGIN;
	}

	private int panelRight() {
		return panelLeft() + PANEL_WIDTH;
	}

	private int panelHeight() {
		return HEADER_HEIGHT + ROWS * ROW + 6 + QuakeTheme.BUTTON_HEIGHT * 2 + BUTTON_GAP + PAD * 2;
	}

	private int panelTop() {
		return (height - panelHeight()) / 2;
	}

	private int rowTop(int row) {
		return panelTop() + PAD + HEADER_HEIGHT + row * ROW;
	}

	private int controlLeft() {
		return panelRight() - PAD - CONTROL_WIDTH;
	}

	@Override
	protected void init() {
		cycle(0, () -> settings.setCrosshairShape(QuakeSettings.previous(settings.crosshairShape())),
				() -> settings.setCrosshairShape(QuakeSettings.next(settings.crosshairShape())));

		CrosshairColor[] colors = CrosshairColor.values();
		int swatchesWidth = colors.length * SWATCH + (colors.length - 1) * SWATCH_GAP;
		int swatchX = controlLeft() + (CONTROL_WIDTH - swatchesWidth) / 2;
		int swatchY = rowTop(1) + (ROW - SWATCH) / 2 - 1;
		for (CrosshairColor color : colors) {
			addRenderableWidget(new ColorSwatchButton(swatchX, swatchY, SWATCH, color.argb(), color.label(),
					() -> settings.crosshairColor() == color, () -> settings.setCrosshairColor(color)));
			swatchX += SWATCH + SWATCH_GAP;
		}

		cycle(2, () -> settings.setHitMarkerShape(QuakeSettings.previous(settings.hitMarkerShape())),
				() -> settings.setHitMarkerShape(QuakeSettings.next(settings.hitMarkerShape())));
		Runnable toggleRing = () -> settings.setCooldownRing(!settings.cooldownRing());
		cycle(3, toggleRing, toggleRing);
		Runnable toggleSidebar = () -> ServerSettings.toggle(ServerSettings.SIDEBAR);
		cycle(4, toggleSidebar, toggleSidebar);
		Runnable toggleRejoin = () -> ServerSettings.toggle(ServerSettings.AUTO_REJOIN);
		cycle(5, toggleRejoin, toggleRejoin);

		int buttonY = rowTop(ROWS) + 6;
		addRenderableWidget(new PopupButton(panelLeft() + PAD, buttonY, PANEL_WIDTH - PAD * 2, QuakeTheme.BUTTON_HEIGHT,
				Component.literal("적중 미리보기"), PopupButton.Style.OUTLINE, this::previewHit));
		addRenderableWidget(new PopupButton(panelLeft() + PAD, buttonY + QuakeTheme.BUTTON_HEIGHT + BUTTON_GAP,
				PANEL_WIDTH - PAD * 2, QuakeTheme.BUTTON_HEIGHT, Component.literal("효과음 설정 ▶"),
				PopupButton.Style.OUTLINE, () -> minecraft.gui.setScreen(new SoundSettingsScreen(this))));
	}

	/** 값 줄의 ◀ ▶ 버튼. 값 글자는 {@link #extractRenderState}에서 가운데에 그립니다. */
	private void cycle(int row, Runnable previous, Runnable next) {
		int y = rowTop(row) + (ROW - ARROW) / 2 - 1;
		addRenderableWidget(new ArrowButton(controlLeft(), y, ARROW, false, previous));
		addRenderableWidget(new ArrowButton(controlLeft() + CONTROL_WIDTH - ARROW, y, ARROW, true, next));
	}

	private void previewHit() {
		previewZone = previewZone == Zone.HEAD ? Zone.BODY : Zone.HEAD;
		previewStartNanos = System.nanoTime();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (QuickSettingsKey.key().matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// 게임 화면을 그대로 보여 주기 위해 바닐라의 흐림·어둡게 하기를 하지 않습니다.
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractPanel(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		extractCenterPreview(graphics);
	}

	private void extractPanel(GuiGraphicsExtractor graphics) {
		int left = panelLeft();
		int right = panelRight();
		int top = panelTop();
		int bottom = top + panelHeight();
		graphics.fill(left + 2, top + 2, right + 2, bottom + 2, QuakeTheme.HUD_SHADOW);
		graphics.fill(left, top, right, bottom, QuakeTheme.PANEL);
		graphics.outline(left, top, PANEL_WIDTH, panelHeight(), QuakeTheme.PANEL_BORDER);

		int titleY = top + PAD + 2;
		graphics.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + PAD, titleY, QuakeTheme.TEXT, false);
		Component hint = Component.empty().append(QuickSettingsKey.key().getTranslatedKeyMessage())
				.append(" 열기 · ESC 닫기");
		graphics.text(font, hint, right - PAD - font.width(hint), titleY, QuakeTheme.TEXT_MUTED, false);

		String[] labels = {"조준점", "조준점 색", "적중 표시", "쿨다운 표시", "사이드바", "자동 다시 참여"};
		String[] values = {settings.crosshairShape().label(), null, hitMarkerLabel(),
				settings.cooldownRing() ? "조준점 링" : "끄기",
				onOff(ServerSettings.bool(ServerSettings.SIDEBAR)),
				onOff(ServerSettings.bool(ServerSettings.AUTO_REJOIN))};
		// 모드 설정과 서버에 저장하는 설정 사이 구분선
		int dividerY = rowTop(CLIENT_ROWS) - 1;
		graphics.fill(left + PAD, dividerY, right - PAD, dividerY + 1, QuakeTheme.PANEL_BORDER);
		for (int row = 0; row < ROWS; row++) {
			int textY = rowTop(row) + (ROW - font.lineHeight) / 2;
			graphics.text(font, labels[row], left + PAD, textY, QuakeTheme.HUD_LABEL, false);
			if (values[row] != null) {
				int valueX = controlLeft() + (CONTROL_WIDTH - font.width(values[row])) / 2;
				graphics.text(font, values[row], valueX, textY, QuakeTheme.TEXT, false);
			}
		}
	}

	/** 서버 값을 아직 받지 못했으면 "…" */
	private static String onOff(Boolean value) {
		return value == null ? "…" : value ? "켜기" : "끄기";
	}

	/** 기획 시안 이름과 같게 "A 클래식 X"처럼 앞에 글자를 붙입니다. */
	private String hitMarkerLabel() {
		return (char) ('A' + settings.hitMarkerShape().ordinal()) + " " + settings.hitMarkerShape().label();
	}

	/** 실제 조준점 자리에 고른 조준점·쿨다운 링·히트마커를 그립니다. HUD 조준점은 이 화면이 열려 있는 동안 숨깁니다. */
	private void extractCenterPreview(GuiGraphicsExtractor graphics) {
		int cx = RailgunCrosshairHud.centerX(graphics);
		int cy = RailgunCrosshairHud.centerY(graphics);
		float cooldownProgress = -1f;
		if (settings.cooldownRing()) {
			long t = System.currentTimeMillis() % COOLDOWN_LOOP_MS;
			cooldownProgress = t < COOLDOWN_MS ? (float) t / COOLDOWN_MS : -1f;
		}
		RailgunCrosshairHud.drawPreview(graphics, cx, cy, settings.crosshairShape(), settings.crosshairColor().argb(),
				cooldownProgress);
		long hitElapsed = System.nanoTime() - previewStartNanos;
		if (previewStartNanos != 0 && hitElapsed < HitMarkerHud.DURATION_NANOS) {
			HitMarkerHud.draw(graphics, cx, cy, settings.hitMarkerShape(), previewZone, hitElapsed);
		}
		String caption = "가운데 조준점에 바로 반영";
		graphics.text(font, caption, cx - font.width(caption) / 2, cy + 24, QuakeTheme.TEXT, true);
	}
}
