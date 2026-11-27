package moe.mchdf.quake.client.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** 색 하나를 고르는 동그라미 버튼입니다. 선택된 색은 노란 고리로 감쌉니다. */
public class ColorSwatchButton extends AbstractButton {
	private final int color;
	private final BooleanSupplier selected;
	private final Runnable onPress;

	/** {@code size}는 고리까지 포함한 지름(홀수 권장)입니다. */
	public ColorSwatchButton(int x, int y, int size, int color, String name, BooleanSupplier selected,
							 Runnable onPress) {
		super(x, y, size, size, Component.literal(name));
		this.color = color;
		this.selected = selected;
		this.onPress = onPress;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		onPress.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int r = getWidth() / 2;
		int cx = getX() + r;
		int cy = getY() + r;
		if (selected.getAsBoolean()) {
			fillCircle(graphics, cx, cy, r, QuakeTheme.HIGHLIGHT);
			fillCircle(graphics, cx, cy, r - 1, QuakeTheme.PANEL);
		} else if (isHoveredOrFocused()) {
			fillCircle(graphics, cx, cy, r, QuakeTheme.TEXT_MUTED);
			fillCircle(graphics, cx, cy, r - 1, QuakeTheme.PANEL);
		}
		fillCircle(graphics, cx, cy, r - 2, color);
	}

	/** 중심 픽셀 ({@code cx}, {@code cy}), 반지름 {@code r}인 꽉 찬 원 */
	static void fillCircle(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.round(Math.sqrt(r * r - dy * dy + r * 0.8));
			half = Math.min(half, r);
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
