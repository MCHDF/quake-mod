package moe.mchdf.quake.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** 바닐라 돌 버튼 대신 평평한 색으로 그리는 팝업용 버튼입니다. */
public class PopupButton extends AbstractButton {
	/** PRIMARY 강조색 바탕, SECONDARY 회색 바탕, GHOST 바탕 없음, OUTLINE 테두리만 있고 글자는 노랑 */
	public enum Style { PRIMARY, SECONDARY, GHOST, OUTLINE }

	private final Style style;
	private final Runnable onPress;

	public PopupButton(int x, int y, int width, int height, Component label, Style style, Runnable onPress) {
		super(x, y, width, height, label);
		this.style = style;
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
		int right = x + getWidth();
		int bottom = y + getHeight();
		boolean hover = isHoveredOrFocused();

		int textColor;
		switch (style) {
			case PRIMARY -> {
				graphics.fill(x, y, right, bottom, hover ? QuakeTheme.ACCENT_HOVER : QuakeTheme.ACCENT);
				textColor = QuakeTheme.TEXT_ON_ACCENT;
			}
			case SECONDARY -> {
				graphics.fill(x, y, right, bottom, hover ? QuakeTheme.BUTTON_HOVER : QuakeTheme.BUTTON);
				graphics.outline(x, y, getWidth(), getHeight(), QuakeTheme.BUTTON_BORDER);
				textColor = QuakeTheme.TEXT;
			}
			case OUTLINE -> {
				if (hover) {
					graphics.fill(x, y, right, bottom, QuakeTheme.BUTTON);
				}
				graphics.outline(x, y, getWidth(), getHeight(), hover ? QuakeTheme.TEXT_MUTED : QuakeTheme.BUTTON_BORDER);
				textColor = QuakeTheme.HIGHLIGHT;
			}
			default -> {
				if (hover) {
					graphics.fill(x, y, right, bottom, QuakeTheme.BUTTON_HOVER);
				}
				textColor = hover ? QuakeTheme.TEXT : QuakeTheme.TEXT_MUTED;
			}
		}
		if (!active) {
			textColor = QuakeTheme.fade(textColor, 0.4f);
		}

		Font font = Minecraft.getInstance().font;
		int textX = x + (getWidth() - font.width(getMessage())) / 2;
		int textY = y + (getHeight() - font.lineHeight) / 2 + 1;
		graphics.text(font, getMessage(), textX, textY, textColor, false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
