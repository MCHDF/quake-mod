package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 서버 → 클라: 사이드바 내용입니다. 바닐라 사이드바 대신 모드 HUD로 그립니다.
 * 형식: boolean 표시 여부, 문자열 제목, 문자열 목록 줄. 문자열은 텍스트 컴포넌트 JSON입니다.
 * 플러그인의 {@code moe.mchdf.quake.mod.ModSidebar#encode}와 형식이 같아야 합니다.
 */
public record SidebarS2CPayload(boolean visible, String title, List<String> lines) implements CustomPacketPayload {
	/** 서버가 보내는 줄 수의 상한입니다. 바닐라 사이드바도 15줄까지만 표시합니다. */
	public static final int MAX_LINES = 32;

	public static final Type<SidebarS2CPayload> TYPE = new Type<>(QuakeProtocol.SIDEBAR);
	public static final StreamCodec<ByteBuf, SidebarS2CPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SidebarS2CPayload::visible,
			ByteBufCodecs.STRING_UTF8, SidebarS2CPayload::title,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_LINES)), SidebarS2CPayload::lines,
			SidebarS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
