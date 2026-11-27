package moe.mchdf.quake.client;

import moe.mchdf.quake.client.hud.SidebarHud;
import net.fabricmc.api.ClientModInitializer;

public class QuakeModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		QuakeServerConnection.register();
		QuakeTestKey.register();
		SidebarHud.register();
	}
}
