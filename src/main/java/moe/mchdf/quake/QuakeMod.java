package moe.mchdf.quake;

import moe.mchdf.quake.network.HandshakeC2SPayload;
import moe.mchdf.quake.network.HandshakeS2CPayload;
import moe.mchdf.quake.network.HitS2CPayload;
import moe.mchdf.quake.network.OpenSettingsS2CPayload;
import moe.mchdf.quake.network.PlayerSettingC2SPayload;
import moe.mchdf.quake.network.PlayerSettingsS2CPayload;
import moe.mchdf.quake.network.SidebarS2CPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuakeMod implements ModInitializer {
	public static final String MOD_ID = "quake";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 패킷 종류는 보내는 쪽과 받는 쪽 모두에 등록해야 합니다.
		PayloadTypeRegistry.serverboundPlay().register(HandshakeC2SPayload.TYPE, HandshakeC2SPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HandshakeS2CPayload.TYPE, HandshakeS2CPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SidebarS2CPayload.TYPE, SidebarS2CPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HitS2CPayload.TYPE, HitS2CPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenSettingsS2CPayload.TYPE, OpenSettingsS2CPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PlayerSettingsS2CPayload.TYPE, PlayerSettingsS2CPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PlayerSettingC2SPayload.TYPE, PlayerSettingC2SPayload.CODEC);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
