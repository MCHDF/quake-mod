package moe.mchdf.quake.network;

import moe.mchdf.quake.QuakeMod;
import net.minecraft.resources.Identifier;

/**
 * 플러그인과 모드가 함께 쓰는 프로토콜 정보입니다.
 * 플러그인의 {@code moe.mchdf.quake.mod.ModProtocol}과 값이 같아야 합니다.
 */
public final class QuakeProtocol {
	/** 패킷 형식이 바뀌면 올립니다. 서버와 다르면 모드를 활성화하지 않습니다. */
	public static final int VERSION = 5;

	/** 핸드셰이크 채널. 클라 → 서버(모드 버전), 서버 → 클라(서버 버전, 수락 여부) 모두 이 채널을 씁니다. */
	public static final Identifier HANDSHAKE = QuakeMod.id("handshake");

	/** 사이드바 채널. 서버 → 클라: 표시 여부, 제목, 줄 목록(텍스트 컴포넌트 JSON). */
	public static final Identifier SIDEBAR = QuakeMod.id("sidebar");

	/** 명중 채널. 서버 → 클라(쏜 사람): 맞힌 부위. 판정은 서버가 하고 모드는 히트마커만 그립니다. */
	public static final Identifier HIT = QuakeMod.id("hit");

	/** 설정 화면 열기 채널. 서버 → 클라: 내용 없음. 설정 아이템을 우클릭하거나 /settings를 쓰면 옵니다. */
	public static final Identifier OPEN_SETTINGS = QuakeMod.id("open_settings");

	/**
	 * 플레이어 설정 채널. 서버 → 클라: 서버 DB에 저장된 설정 전체. 클라 → 서버: 바꾼 항목 하나.
	 * 사이드바처럼 서버가 적용하는 설정은 바닐라 사용자와 같이 서버에 저장합니다.
	 */
	public static final Identifier PLAYER_SETTINGS = QuakeMod.id("player_settings");

	private QuakeProtocol() {
	}
}
