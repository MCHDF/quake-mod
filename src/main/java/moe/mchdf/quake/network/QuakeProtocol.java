package moe.mchdf.quake.network;

import moe.mchdf.quake.QuakeMod;
import net.minecraft.resources.Identifier;

/**
 * 플러그인과 모드가 함께 쓰는 프로토콜 정보입니다.
 * 플러그인의 {@code moe.mchdf.quake.mod.ModProtocol}과 값이 같아야 합니다.
 */
public final class QuakeProtocol {
	/** 패킷 형식이 바뀌면 올립니다. 서버와 다르면 모드를 활성화하지 않습니다. */
	public static final int VERSION = 1;

	/** 핸드셰이크 채널. 클라 → 서버(모드 버전), 서버 → 클라(서버 버전, 수락 여부) 모두 이 채널을 씁니다. */
	public static final Identifier HANDSHAKE = QuakeMod.id("handshake");

	private QuakeProtocol() {
	}
}
