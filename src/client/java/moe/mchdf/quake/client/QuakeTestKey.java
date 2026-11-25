package moe.mchdf.quake.client;

import com.mojang.blaze3d.platform.InputConstants;
import moe.mchdf.quake.client.screen.QuakeTestScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 테스트용 단축키: Ctrl+F를 누르면 테스트 GUI를 엽니다.
 *
 * <p>모드가 활성화된 경우(Quake 플러그인 서버와 핸드셰이크 완료)에만 화면이 열리고,
 * 비활성 상태에서는 화면 대신 액션바에 비활성 안내만 띄웁니다.
 *
 * <p>F는 바닐라 "양손 아이템 교체" 키라서, Ctrl과 함께 누른 경우에는 교체 입력을 버려 아이템이 바뀌지 않게 합니다.
 * 바닐라 키 처리보다 먼저 실행되도록 틱 시작 시점에 확인합니다.
 */
final class QuakeTestKey {
	private static boolean wasDown;

	private QuakeTestKey() {
	}

	static void register() {
		ClientTickEvents.START_CLIENT_TICK.register(QuakeTestKey::onTick);
	}

	private static void onTick(Minecraft client) {
		boolean down = client.player != null
				&& client.gui.screen() == null
				&& client.hasControlDown()
				&& InputConstants.isKeyDown(InputConstants.KEY_F);
		if (down) {
			while (client.options.keySwapOffhand.consumeClick()) {
				// Ctrl+F는 테스트 GUI 전용으로 쓰고 양손 교체는 막습니다.
			}
		}
		if (down && !wasDown) {
			open(client);
		}
		wasDown = down;
	}

	private static void open(Minecraft client) {
		if (QuakeServerConnection.isActive()) {
			client.gui.setScreen(new QuakeTestScreen());
		} else {
			client.player.sendOverlayMessage(Component.literal("[Quake] 모드 비활성 상태라 테스트 GUI를 열지 않습니다."));
		}
	}
}
