package moe.mchdf.quake.client.screen;

import java.util.function.Supplier;
import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.hud.HitMarkerHud;
import moe.mchdf.quake.client.hud.RailgunCrosshairHud;
import moe.mchdf.quake.client.settings.QuakeSettings;
import moe.mchdf.quake.client.ui.PopupButton;
import moe.mchdf.quake.client.ui.PopupScreen;
import moe.mchdf.quake.client.ui.QuakeTheme;
import moe.mchdf.quake.network.HitS2CPayload.Zone;
import moe.mchdf.quake.network.OpenSettingsS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 모드 사용자의 내 설정 화면입니다. 서버가 {@link OpenSettingsS2CPayload}를 보내면 엽니다
 * (설정 아이템 우클릭, {@code /settings}).
 *
 * <p>왼쪽에는 항목마다 이름과 값 버튼이 있고, 버튼을 누르면 다음 값으로 바뀌며 바로 저장됩니다.
 * 오른쪽 미리보기에는 고른 조준점·쿨다운 링·히트마커가 실제 움직임대로 반복해서 그려집니다.
 * 설정은 모드 사용자 전용이라 클라이언트에만 저장합니다({@link QuakeSettings}).
 */
public class SettingsScreen extends PopupScreen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 168;
	private static final int ROW = 22;
	private static final int BUTTON_WIDTH = 104;
	private static final int PREVIEW_SIZE = 84;
	/** 미리보기는 GUI 픽셀 하나를 이만큼 키워 그립니다. */
	private static final float PREVIEW_SCALE = 2f;
	/** 미리보기 반복 주기: 쿨다운 1.8초가 차오른 뒤 잠깐 쉬고, 히트마커는 처치·헤드샷을 번갈아 보여 줍니다. */
	private static final long COOLDOWN_LOOP_MS = 2600;
	private static final long COOLDOWN_MS = 1800;
	private static final long HIT_LOOP_MS = 1300;

	private final QuakeSettings settings = QuakeSettings.get();

	public SettingsScreen() {
		super(Component.literal("설정"), WIDTH, HEIGHT);
	}

	/** 서버의 설정 열기 패킷을 받으면 화면을 엽니다. 모드가 비활성(서버와 버전이 다름)이면 열지 않습니다. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(OpenSettingsS2CPayload.TYPE,
				(payload, context) -> open(context.client()));
	}

	private static void open(Minecraft client) {
		if (QuakeServerConnection.isActive()) {
			client.gui.setScreen(new SettingsScreen());
		}
	}

	@Override
	protected void initContent() {
		int buttonX = previewLeft() - QuakeTheme.PADDING - BUTTON_WIDTH;
		int y = firstRowTop();
		option(buttonX, y, () -> settings.crosshairShape().label(),
				() -> settings.setCrosshairShape(QuakeSettings.next(settings.crosshairShape())));
		option(buttonX, y += ROW, () -> settings.crosshairColor().label(),
				() -> settings.setCrosshairColor(QuakeSettings.next(settings.crosshairColor())));
		option(buttonX, y += ROW, () -> settings.hitMarkerShape().label(),
				() -> settings.setHitMarkerShape(QuakeSettings.next(settings.hitMarkerShape())));
		option(buttonX, y += ROW, () -> settings.cooldownRing() ? "켜기" : "끄기",
				() -> settings.setCooldownRing(!settings.cooldownRing()));

		int closeY = panelBottom() - QuakeTheme.PADDING - QuakeTheme.BUTTON_HEIGHT;
		addRenderableWidget(new PopupButton(contentLeft(), closeY, contentWidth(), QuakeTheme.BUTTON_HEIGHT,
				Component.literal("닫기"), PopupButton.Style.SECONDARY, this::onClose));
	}

	/** 값 버튼 하나. 누르면 값을 바꾸고 버튼 글자를 새 값으로 고칩니다. */
	private void option(int x, int y, Supplier<String> label, Runnable change) {
		PopupButton[] holder = new PopupButton[1];
		holder[0] = new PopupButton(x, y, BUTTON_WIDTH, QuakeTheme.BUTTON_HEIGHT, Component.literal(label.get()),
				PopupButton.Style.SECONDARY, () -> {
			change.run();
			holder[0].setMessage(Component.literal(label.get()));
		});
		addRenderableWidget(holder[0]);
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = contentLeft();
		int textOffset = (QuakeTheme.BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
		graphics.text(font, "레일건 화면", x, contentTop(), QuakeTheme.TEXT_MUTED, false);

		String[] labels = {"조준점 모양", "조준점 색", "히트마커 모양", "쿨다운 링"};
		int y = firstRowTop();
		for (String label : labels) {
			graphics.text(font, label, x, y + textOffset, QuakeTheme.TEXT, false);
			y += ROW;
		}
		// 조준점 색 버튼 옆에 색 견본
		int swatchX = previewLeft() - QuakeTheme.PADDING - BUTTON_WIDTH - 12;
		int swatchY = firstRowTop() + ROW + (QuakeTheme.BUTTON_HEIGHT - 8) / 2;
		graphics.fill(swatchX - 1, swatchY - 1, swatchX + 9, swatchY + 9, QuakeTheme.PANEL_BORDER);
		graphics.fill(swatchX, swatchY, swatchX + 8, swatchY + 8, settings.crosshairColor().argb());

		graphics.text(font, "레일건을 들었을 때만 바뀝니다.", x, y + 2, QuakeTheme.TEXT_MUTED, false);
		extractPreview(graphics);
	}

	private void extractPreview(GuiGraphicsExtractor graphics) {
		int left = previewLeft();
		int top = contentTop();
		graphics.fill(left, top, left + PREVIEW_SIZE, top + PREVIEW_SIZE, 0xFF5B7A96);
		// 하늘색 위에 잔디 띠를 깔아 밝은 배경과 어두운 배경에서 모두 어떻게 보이는지 보여 줍니다.
		graphics.fill(left, top + PREVIEW_SIZE * 2 / 3, left + PREVIEW_SIZE, top + PREVIEW_SIZE, 0xFF4F7A33);
		graphics.outline(left, top, PREVIEW_SIZE, PREVIEW_SIZE, QuakeTheme.PANEL_BORDER);
		graphics.text(font, "미리보기", left + 3, top + 3, QuakeTheme.TEXT, true);

		long now = System.currentTimeMillis();
		float cooldownProgress = -1f;
		if (settings.cooldownRing()) {
			long t = now % COOLDOWN_LOOP_MS;
			cooldownProgress = t < COOLDOWN_MS ? (float) t / COOLDOWN_MS : -1f;
		}
		long hitTime = now % HIT_LOOP_MS;
		Zone zone = (now / HIT_LOOP_MS) % 2 == 0 ? Zone.BODY : Zone.HEAD;

		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(left + PREVIEW_SIZE / 2f, top + PREVIEW_SIZE / 2f + 4);
		pose.scale(PREVIEW_SCALE, PREVIEW_SCALE);
		RailgunCrosshairHud.drawPreview(graphics, 0, 0, settings.crosshairShape(), settings.crosshairColor().argb(),
				cooldownProgress);
		long hitElapsed = hitTime * 1_000_000L;
		if (hitElapsed < HitMarkerHud.DURATION_NANOS) {
			HitMarkerHud.draw(graphics, 0, 0, settings.hitMarkerShape(), zone, hitElapsed);
		}
		pose.popMatrix();

		String caption = zone == Zone.HEAD ? "헤드샷" : "처치";
		graphics.text(font, caption, left + (PREVIEW_SIZE - font.width(caption)) / 2,
				top + PREVIEW_SIZE + 4, QuakeTheme.TEXT_MUTED, false);
	}

	private int previewLeft() {
		return contentLeft() + contentWidth() - PREVIEW_SIZE;
	}

	private int firstRowTop() {
		return contentTop() + font.lineHeight + 6;
	}
}
