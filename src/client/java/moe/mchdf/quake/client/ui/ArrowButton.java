package moe.mchdf.quake.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** 값을 이전·다음으로 넘기는 작은 정사각형 화살표 버튼입니다. 화살표는 글꼴 대신 픽셀로 그립니다. */
public class ArrowButton extends AbstractButton {
	private final boolean right;
	private final Runnable onPress;

	public ArrowButton(int x, int y, int size, boolean right, Runnable onPress) {
		super(x, y, size, size, Component.literal(right ? "다음" : "이전"));
		this.right = right;
		this.onPress = onPress;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		onPress.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		graphics.fill(x, y, x + getWidth(), y + getHeight(), isHoveredOrFocused() ? QuakeTheme.BUTTON_HOVER : QuakeTheme.BUTTON);
		// 높이 7픽셀짜리 삼각형: 뾰족한 쪽부터 한 줄씩 길어집니다.
		int half = 3;
		int cx = x + getWidth() / 2;
		int cy = y + getHeight() / 2;
		for (int i = 0; i <= half; i++) {
			int column = right ? cx + 1 - i : cx - 2 + i;
			graphics.fill(column, cy - i, column + 1, cy + i + 1, QuakeTheme.TEXT);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
