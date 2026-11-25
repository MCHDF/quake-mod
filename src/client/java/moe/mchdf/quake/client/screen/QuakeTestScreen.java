package moe.mchdf.quake.client.screen;

import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.ui.PopupButton;
import moe.mchdf.quake.client.ui.PopupScreen;
import moe.mchdf.quake.client.ui.QuakeTheme;
import moe.mchdf.quake.network.QuakeProtocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 팝업 스타일 테스트 화면입니다. 모드 활성화 여부와 함께 글자·구분선·버튼 종류를 한 번에 보여 줍니다.
 * 실제 GUI를 만들면 지웁니다.
 */
public class QuakeTestScreen extends PopupScreen {
	private static final int WIDTH = 220;
	private static final int HEIGHT = 150;
	private static final int ROW = 13;

	public QuakeTestScreen() {
		super(Component.literal("Quake 테스트"), WIDTH, HEIGHT);
	}

	@Override
	protected void initContent() {
		int gap = 6;
		int buttonWidth = (contentWidth() - gap * 2) / 3;
		int y = panelBottom() - QuakeTheme.PADDING - QuakeTheme.BUTTON_HEIGHT;
		int x = contentLeft();
		addRenderableWidget(new PopupButton(x, y, buttonWidth, QuakeTheme.BUTTON_HEIGHT,
				Component.literal("확인"), PopupButton.Style.PRIMARY, this::onClose));
		addRenderableWidget(new PopupButton(x + buttonWidth + gap, y, buttonWidth, QuakeTheme.BUTTON_HEIGHT,
				Component.literal("보조"), PopupButton.Style.SECONDARY, () -> { }));
		addRenderableWidget(new PopupButton(x + (buttonWidth + gap) * 2, y, buttonWidth, QuakeTheme.BUTTON_HEIGHT,
				Component.literal("취소"), PopupButton.Style.GHOST, this::onClose));
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = contentLeft();
		int y = contentTop();

		graphics.text(font, "서버 연결 상태", x, y, QuakeTheme.TEXT_MUTED, false);
		y += ROW + 2;

		boolean active = QuakeServerConnection.isActive();
		row(graphics, x, y, "모드", active ? "활성화" : "비활성", active ? QuakeTheme.SUCCESS : QuakeTheme.DANGER);
		y += ROW;
		row(graphics, x, y, "모드 프로토콜", String.valueOf(QuakeProtocol.VERSION), QuakeTheme.TEXT);
		y += ROW;
		row(graphics, x, y, "서버 프로토콜", String.valueOf(QuakeServerConnection.serverProtocolVersion()), QuakeTheme.TEXT);
		y += ROW + 2;

		graphics.horizontalLine(x, x + contentWidth() - 1, y, QuakeTheme.DIVIDER);
		y += 6;
		graphics.text(font, "버튼 스타일: 주 / 보조 / 고스트", x, y, QuakeTheme.TEXT_MUTED, false);
	}

	/** 왼쪽에 이름, 오른쪽 끝에 값을 그립니다. */
	private void row(GuiGraphicsExtractor graphics, int x, int y, String label, String value, int valueColor) {
		graphics.text(font, label, x, y, QuakeTheme.TEXT, false);
		graphics.text(font, value, x + contentWidth() - font.width(value), y, valueColor, false);
	}
}
