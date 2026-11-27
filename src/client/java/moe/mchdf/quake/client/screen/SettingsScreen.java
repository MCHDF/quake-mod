package moe.mchdf.quake.client.screen;

import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.ui.PopupButton;
import moe.mchdf.quake.client.ui.PopupScreen;
import moe.mchdf.quake.client.ui.QuakeTheme;
import moe.mchdf.quake.network.OpenSettingsS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 모드 사용자의 내 설정 화면입니다. 서버가 {@link OpenSettingsS2CPayload}를 보내면 엽니다
 * (설정 아이템 우클릭, {@code /settings}).
 *
 * <p>모드 사용자는 바닐라 사용자보다 바꿀 수 있는 항목이 많아(조준점, 히트마커 모양 등) 서버의 상자형 GUI 대신
 * 이 화면을 씁니다. 지금은 틀만 있고, 설정 항목은 다음 단계에서 채웁니다.
 */
public class SettingsScreen extends PopupScreen {
	private static final int WIDTH = 240;
	private static final int HEIGHT = 170;
	private static final int ROW = 13;

	/** 앞으로 들어갈 설정 묶음. 항목을 만들면 이 목록 대신 실제 위젯으로 바꿉니다. */
	private static final String[][] SECTIONS = {
			{"전투", "조준점 · 히트마커 · 쿨다운 표시"},
			{"화면", "사이드바 · 처치 알림 · 빔 표시"},
			{"소리", "효과음 음량"},
	};

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
		int y = panelBottom() - QuakeTheme.PADDING - QuakeTheme.BUTTON_HEIGHT;
		addRenderableWidget(new PopupButton(contentLeft(), y, contentWidth(), QuakeTheme.BUTTON_HEIGHT,
				Component.literal("닫기"), PopupButton.Style.SECONDARY, this::onClose));
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = contentLeft();
		int y = contentTop();
		int right = x + contentWidth();

		for (String[] section : SECTIONS) {
			graphics.text(font, section[0], x, y, QuakeTheme.TEXT, false);
			graphics.text(font, section[1], right - font.width(section[1]), y, QuakeTheme.TEXT_MUTED, false);
			y += ROW;
			graphics.horizontalLine(x, right - 1, y - 3, QuakeTheme.DIVIDER);
			y += 4;
		}
		y += 4;
		graphics.text(font, "설정 항목은 곧 추가됩니다.", x, y, QuakeTheme.TEXT_MUTED, false);
	}
}
