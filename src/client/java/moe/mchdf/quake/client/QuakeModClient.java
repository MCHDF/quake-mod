package moe.mchdf.quake.client;

import moe.mchdf.quake.client.hud.HitMarkerHud;
import moe.mchdf.quake.client.hud.RailgunCrosshairHud;
import moe.mchdf.quake.client.hud.SidebarHud;
import moe.mchdf.quake.client.screen.SettingsScreen;
import net.fabricmc.api.ClientModInitializer;

public class QuakeModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		QuakeServerConnection.register();
		QuakeTestKey.register();
		QuickSettingsKey.register();
		SidebarHud.register();
		RailgunCrosshairHud.register();
		HitMarkerHud.register();
		SettingsScreen.register();
	}
}
