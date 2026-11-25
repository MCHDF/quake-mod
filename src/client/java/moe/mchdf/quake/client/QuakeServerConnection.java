package moe.mchdf.quake.client;

import java.util.List;
import moe.mchdf.quake.QuakeMod;
import moe.mchdf.quake.network.HandshakeC2SPayload;
import moe.mchdf.quake.network.HandshakeS2CPayload;
import moe.mchdf.quake.network.QuakeProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 접속한 서버에 Quake 플러그인이 있는지 확인하고 모드 기능의 활성화 여부를 관리합니다.
 *
 * <p>흐름:
 * <ol>
 *   <li>서버가 {@code quake:handshake} 채널을 등록했다고 알려 오면 모드 버전을 보냅니다.</li>
 *   <li>서버가 수락 응답을 보내면 활성화합니다.</li>
 *   <li>접속이 끊기면 비활성화합니다.</li>
 * </ol>
 * 플러그인이 없는 서버에서는 채널이 등록되지 않으므로 아무 패킷도 보내지 않고 비활성 상태로 남습니다.
 * GUI 등 모드 기능은 {@link #isActive()}가 true일 때만 동작해야 합니다.
 */
public final class QuakeServerConnection {
	private static boolean handshakeSent;
	private static boolean active;
	private static int serverProtocolVersion = -1;

	private QuakeServerConnection() {
	}

	/** 접속한 서버의 Quake 플러그인과 핸드셰이크가 끝났는지 여부입니다. 클라이언트 스레드에서 읽습니다. */
	public static boolean isActive() {
		return active;
	}

	/** 서버가 알려 준 프로토콜 버전입니다. 응답을 받기 전에는 -1입니다. */
	public static int serverProtocolVersion() {
		return serverProtocolVersion;
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(HandshakeS2CPayload.TYPE,
				(payload, context) -> onReply(payload, context.client()));

		// 채널 등록 알림이 접속 완료보다 먼저 올 수도, 나중에 올 수도 있어 두 시점 모두 확인합니다.
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> trySendHandshake());
		ServerboundPlayChannelEvents.REGISTER.register((listener, sender, client, channels) -> {
			if (containsHandshake(channels)) {
				client.execute(QuakeServerConnection::trySendHandshake);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(QuakeServerConnection::reset));
	}

	private static boolean containsHandshake(List<Identifier> channels) {
		return channels.contains(QuakeProtocol.HANDSHAKE);
	}

	private static void trySendHandshake() {
		if (handshakeSent || !ClientPlayNetworking.canSend(HandshakeC2SPayload.TYPE)) {
			return;
		}
		handshakeSent = true;
		ClientPlayNetworking.send(new HandshakeC2SPayload(QuakeProtocol.VERSION));
		QuakeMod.LOGGER.info("Quake 플러그인 채널을 찾아 핸드셰이크를 보냈습니다. (모드 프로토콜 {})", QuakeProtocol.VERSION);
	}

	private static void onReply(HandshakeS2CPayload payload, Minecraft client) {
		serverProtocolVersion = payload.protocolVersion();
		if (payload.accepted() && payload.protocolVersion() == QuakeProtocol.VERSION) {
			active = true;
			QuakeMod.LOGGER.info("Quake 서버와 연결되었습니다. 모드 기능을 활성화합니다. (프로토콜 {})", serverProtocolVersion);
			return;
		}
		active = false;
		QuakeMod.LOGGER.warn("Quake 서버와 프로토콜 버전이 맞지 않습니다. (모드 {}, 서버 {})",
				QuakeProtocol.VERSION, serverProtocolVersion);
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal(
					"[Quake] 모드 버전이 서버와 맞지 않아 모드 기능을 끕니다. (모드 " + QuakeProtocol.VERSION
							+ ", 서버 " + serverProtocolVersion + ")"));
		}
	}

	private static void reset() {
		if (active) {
			QuakeMod.LOGGER.info("Quake 서버와 연결이 끊겨 모드 기능을 비활성화합니다.");
		}
		handshakeSent = false;
		active = false;
		serverProtocolVersion = -1;
	}
}
