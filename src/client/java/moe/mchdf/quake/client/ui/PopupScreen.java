package moe.mchdf.quake.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 게임 화면 위에 뜨는 팝업 창의 기본 틀입니다.
 *
 * <p>바닐라 화면처럼 배경을 흐리게 하거나 흙 배경을 깔지 않고, 게임 화면을 살짝 어둡게 한 뒤
 * 가운데에 제목 줄과 닫기 버튼이 있는 패널을 그립니다. 하위 클래스는 {@link #initContent()}에서
 * 위젯을 추가하고 {@link #extractContent}에서 본문을 그립니다. 좌표는 {@link #contentLeft()} 등을 기준으로 잡습니다.
 */
public abstract class PopupScreen extends Screen {
	private final int panelWidth;
	private final int panelHeight;
	private long openedAt;

	protected PopupScreen(Component title, int panelWidth, int panelHeight) {
		super(title);
		this.panelWidth = panelWidth;
		this.panelHeight = panelHeight;
	}

	protected int panelLeft() {
		return (width - panelWidth) / 2;
	}

	protected int panelTop() {
		return (height - panelHeight) / 2;
	}

	protected int panelRight() {
		return panelLeft() + panelWidth;
	}

	protected int panelBottom() {
		return panelTop() + panelHeight;
	}

	protected int contentLeft() {
		return panelLeft() + QuakeTheme.PADDING;
	}

	protected int contentTop() {
		return panelTop() + QuakeTheme.HEADER_HEIGHT + QuakeTheme.PADDING;
	}

	protected int contentWidth() {
		return panelWidth - QuakeTheme.PADDING * 2;
	}

	@Override
	protected final void init() {
		if (openedAt == 0) {
			openedAt = System.currentTimeMillis();
		}
		int size = QuakeTheme.HEADER_HEIGHT - 6;
		addRenderableWidget(new PopupButton(panelRight() - size - 3, panelTop() + 3, size, size,
				Component.literal("✕"), PopupButton.Style.GHOST, this::onClose));
		initContent();
	}

	/** 패널 안에 들어갈 버튼 등 위젯을 추가합니다. 창 크기가 바뀔 때마다 다시 호출됩니다. */
	protected abstract void initContent();

	/** 패널 본문(글자, 구분선 등)을 그립니다. 패널 배경 위, 위젯과 같은 층에 그려집니다. */
	protected abstract void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// 바닐라의 흐림 효과 대신 게임 화면을 살짝 어둡게만 합니다.
		graphics.fill(0, 0, width, height, QuakeTheme.fade(QuakeTheme.OVERLAY, openProgress()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		float progress = openProgress();
		float scale = 0.92f + 0.08f * easeOut(progress);
		float cx = panelLeft() + panelWidth / 2f;
		float cy = panelTop() + panelHeight / 2f;

		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.scale(scale, scale);
		pose.translate(-cx, -cy);

		extractPanel(graphics);
		extractContent(graphics, mouseX, mouseY, partialTick);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		pose.popMatrix();
	}

	private void extractPanel(GuiGraphicsExtractor graphics) {
		int l = panelLeft();
		int t = panelTop();
		int r = panelRight();
		int b = panelBottom();
		int headerBottom = t + QuakeTheme.HEADER_HEIGHT;

		// 그림자 (오른쪽 아래로 2px)
		graphics.fill(l + 2, t + 2, r + 2, b + 2, 0x60000000);
		// 본문과 제목 줄
		graphics.fill(l, t, r, b, QuakeTheme.PANEL);
		graphics.fill(l, t, r, headerBottom, QuakeTheme.PANEL_HEADER);
		graphics.horizontalLine(l, r - 1, headerBottom, QuakeTheme.DIVIDER);
		// 테두리와 위쪽 강조선
		graphics.outline(l, t, r - l, b - t, QuakeTheme.PANEL_BORDER);
		graphics.fill(l, t, r, t + 2, QuakeTheme.ACCENT);
		// 제목
		graphics.text(font, title, l + QuakeTheme.PADDING, t + (QuakeTheme.HEADER_HEIGHT - font.lineHeight) / 2 + 1,
				QuakeTheme.TEXT, false);
	}

	private float openProgress() {
		if (QuakeTheme.OPEN_ANIMATION_MS <= 0 || openedAt == 0) {
			return 1f;
		}
		return Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) QuakeTheme.OPEN_ANIMATION_MS);
	}

	private static float easeOut(float t) {
		float inv = 1f - t;
		return 1f - inv * inv * inv;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
