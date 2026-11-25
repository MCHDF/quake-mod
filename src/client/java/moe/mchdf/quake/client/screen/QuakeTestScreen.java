package moe.mchdf.quake.client.screen;

import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.network.QuakeProtocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 모드 활성화 여부를 확인하기 위한 테스트 화면입니다. 실제 GUI를 만들면 지웁니다. */
public class QuakeTestScreen extends Screen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int GRAY = 0xFFAAAAAA;

	public QuakeTestScreen() {
		super(Component.literal("Quake 테스트 GUI"));
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("닫기"), button -> onClose())
				.bounds(width / 2 - 50, height / 2 + 30, 100, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int y = height / 2 - 40;
		centered(graphics, title.getString(), y, WHITE);
		centered(graphics, "모드 활성화됨: " + QuakeServerConnection.isActive(), y + 20, GREEN);
		centered(graphics, "프로토콜  모드 " + QuakeProtocol.VERSION
				+ " / 서버 " + QuakeServerConnection.serverProtocolVersion(), y + 34, GRAY);
	}

	private void centered(GuiGraphicsExtractor graphics, String text, int y, int color) {
		graphics.text(font, text, (width - font.width(text)) / 2, y, color);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
