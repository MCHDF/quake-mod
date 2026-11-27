package moe.mchdf.quake.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 서버 → 클라: 사이드바 내용입니다. 바닐라 사이드바 대신 모드 HUD로 그립니다.
 *
 * <p>형식: boolean 표시 여부, 문자열 제목, int 강조색(ARGB, 0이면 기본색), 줄 목록.
 * 줄은 byte 종류 다음에 종류별 값이 옵니다. 글자는 모두 텍스트 컴포넌트 JSON입니다.
 * 플러그인의 {@code moe.mchdf.quake.mod.ModSidebar#encode}와 형식이 같아야 합니다.
 */
public record SidebarS2CPayload(boolean visible, String title, int accent, List<Row> rows)
		implements CustomPacketPayload {
	/** 서버가 보내는 줄 수의 상한입니다. */
	public static final int MAX_ROWS = 32;

	private static final int ROW_SPACER = 0;
	private static final int ROW_TEXT = 1;
	private static final int ROW_VALUE = 2;
	private static final int ROW_BAR = 3;

	/** 사이드바 한 줄. 문구와 색은 서버가 정하고 모드는 형태에 맞춰 그리기만 합니다. */
	public sealed interface Row {
		/** 구분선 */
		record Spacer() implements Row {
		}

		/** 글자만 있는 줄 */
		record Text(String text, boolean centered) implements Row {
		}

		/** 왼쪽 이름, 오른쪽 값 */
		record Value(String label, String value) implements Row {
		}

		/** 이름과 값 아래에 진행 막대. ratio는 0~1, color는 ARGB */
		record Bar(String label, String value, float ratio, int color) implements Row {
		}
	}

	public static final StreamCodec<ByteBuf, Row> ROW_CODEC = StreamCodec.of(SidebarS2CPayload::writeRow,
			SidebarS2CPayload::readRow);

	public static final Type<SidebarS2CPayload> TYPE = new Type<>(QuakeProtocol.SIDEBAR);
	public static final StreamCodec<ByteBuf, SidebarS2CPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SidebarS2CPayload::visible,
			ByteBufCodecs.STRING_UTF8, SidebarS2CPayload::title,
			ByteBufCodecs.INT, SidebarS2CPayload::accent,
			ROW_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)), SidebarS2CPayload::rows,
			SidebarS2CPayload::new);

	private static void writeRow(ByteBuf buf, Row row) {
		switch (row) {
			case Row.Spacer spacer -> buf.writeByte(ROW_SPACER);
			case Row.Text text -> {
				buf.writeByte(ROW_TEXT);
				ByteBufCodecs.STRING_UTF8.encode(buf, text.text());
				buf.writeBoolean(text.centered());
			}
			case Row.Value value -> {
				buf.writeByte(ROW_VALUE);
				ByteBufCodecs.STRING_UTF8.encode(buf, value.label());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.value());
			}
			case Row.Bar bar -> {
				buf.writeByte(ROW_BAR);
				ByteBufCodecs.STRING_UTF8.encode(buf, bar.label());
				ByteBufCodecs.STRING_UTF8.encode(buf, bar.value());
				buf.writeFloat(bar.ratio());
				buf.writeInt(bar.color());
			}
		}
	}

	private static Row readRow(ByteBuf buf) {
		int type = buf.readByte();
		return switch (type) {
			case ROW_SPACER -> new Row.Spacer();
			case ROW_TEXT -> new Row.Text(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readBoolean());
			case ROW_VALUE -> new Row.Value(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf));
			case ROW_BAR -> new Row.Bar(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
					Math.clamp(buf.readFloat(), 0f, 1f), buf.readInt());
			default -> throw new DecoderException("알 수 없는 사이드바 줄 종류: " + type);
		};
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
