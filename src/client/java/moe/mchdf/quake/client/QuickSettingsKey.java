package moe.mchdf.quake.client;

import com.mojang.blaze3d.platform.InputConstants;
import moe.mchdf.quake.QuakeMod;
import moe.mchdf.quake.client.screen.SettingsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * 빠른 설정 단축키(기본 F8). 바닐라 조작 설정 화면의 Quake 항목에서 다른 키로 바꿀 수 있습니다.
 * F8은 바닐라에서 기본으로 묶인 기능이 없습니다(시네마틱 카메라도 기본은 키 없음).
 * 빠른 설정이 열려 있을 때 같은 키를 누르면 닫는 처리는 {@link SettingsScreen}이 합니다.
 */
public final class QuickSettingsKey {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(QuakeMod.id("main"));
	private static KeyMapping key;

	private QuickSettingsKey() {
	}

	public static KeyMapping key() {
		return key;
	}

	static void register() {
		key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.quake.quick_settings",
				InputConstants.KEY_F8, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(QuickSettingsKey::onTick);
	}

	private static void onTick(Minecraft client) {
		while (key.consumeClick()) {
			if (client.player != null && client.gui.screen() == null && QuakeServerConnection.isActive()) {
				SettingsScreen.open(client);
			}
		}
	}
}
