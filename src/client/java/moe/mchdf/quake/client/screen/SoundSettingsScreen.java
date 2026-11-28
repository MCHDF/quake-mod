package moe.mchdf.quake.client.screen;

import moe.mchdf.quake.client.settings.ServerSettings;
import moe.mchdf.quake.client.settings.ServerSettings.SoundType;
import moe.mchdf.quake.client.ui.ArrowButton;
import moe.mchdf.quake.client.ui.PopupButton;
import moe.mchdf.quake.client.ui.QuakeTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 빠른 설정의 효과음 분류. 종류마다 음량(끄기·작게·보통·크게)을 ◀▶로 바꿉니다.
 *
 * <p>값은 바닐라 사용자와 같이 서버 DB에 저장되고({@link ServerSettings}), 바꿀 때마다 서버가 그 소리를 새 음량으로
 * 들려줍니다. 빠른 설정과 같은 왼쪽 패널 모양이며, ESC나 「뒤로」를 누르면 빠른 설정으로 돌아갑니다.
 */
public class SoundSettingsScreen extends Screen {
	private static final int MARGIN = 12;
	private static final int PANEL_WIDTH = 200;
	private static final int PAD = 10;
	private static final int HEADER_HEIGHT = 22;
	private static final int ROW = 20;
	private static final int ARROW = 14;
	private static final int CONTROL_WIDTH = 92;
	private static final SoundType[] TYPES = SoundType.values();

	private final Screen parent;

	public SoundSettingsScreen(Screen parent) {
		super(Component.literal("효과음"));
		this.parent = parent;
	}

	private int panelHeight() {
		return HEADER_HEIGHT + TYPES.length * ROW + 6 + QuakeTheme.BUTTON_HEIGHT + PAD * 2;
	}

	private int panelTop() {
		return (height - panelHeight()) / 2;
	}

	private int rowTop(int row) {
		return panelTop() + PAD + HEADER_HEIGHT + row * ROW;
	}

	private int controlLeft() {
		return MARGIN + PANEL_WIDTH - PAD - CONTROL_WIDTH;
	}

	@Override
	protected void init() {
		for (int row = 0; row < TYPES.length; row++) {
			SoundType type = TYPES[row];
			int y = rowTop(row) + (ROW - ARROW) / 2 - 1;
			addRenderableWidget(new ArrowButton(controlLeft(), y, ARROW, false,
					() -> ServerSettings.cycleSound(type, false)));
			addRenderableWidget(new ArrowButton(controlLeft() + CONTROL_WIDTH - ARROW, y, ARROW, true,
					() -> ServerSettings.cycleSound(type, true)));
		}
		addRenderableWidget(new PopupButton(MARGIN + PAD, rowTop(TYPES.length) + 6, PANEL_WIDTH - PAD * 2,
				QuakeTheme.BUTTON_HEIGHT, Component.literal("뒤로"), PopupButton.Style.OUTLINE, this::onClose));
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// 빠른 설정처럼 게임 화면을 흐리게 하지 않습니다.
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int left = MARGIN;
		int right = MARGIN + PANEL_WIDTH;
		int top = panelTop();
		int bottom = top + panelHeight();
		graphics.fill(left + 2, top + 2, right + 2, bottom + 2, QuakeTheme.HUD_SHADOW);
		graphics.fill(left, top, right, bottom, QuakeTheme.PANEL);
		graphics.outline(left, top, PANEL_WIDTH, panelHeight(), QuakeTheme.PANEL_BORDER);

		int titleY = top + PAD + 2;
		graphics.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + PAD, titleY, QuakeTheme.TEXT, false);
		String hint = "바꾸면 소리로 들려줌";
		graphics.text(font, hint, right - PAD - font.width(hint), titleY, QuakeTheme.TEXT_MUTED, false);

		for (int row = 0; row < TYPES.length; row++) {
			int textY = rowTop(row) + (ROW - font.lineHeight) / 2;
			graphics.text(font, TYPES[row].label(), left + PAD, textY, QuakeTheme.HUD_LABEL, false);
			String value = ServerSettings.soundLabel(TYPES[row]);
			String shown = value == null ? "…" : value;
			int color = "끄기".equals(value) ? QuakeTheme.TEXT_MUTED : QuakeTheme.TEXT;
			graphics.text(font, shown, controlLeft() + (CONTROL_WIDTH - font.width(shown)) / 2, textY, color, false);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}
}
