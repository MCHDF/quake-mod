package moe.mchdf.quake.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import moe.mchdf.quake.QuakeMod;
import moe.mchdf.quake.client.QuakeServerConnection;
import moe.mchdf.quake.client.ui.QuakeTheme;
import moe.mchdf.quake.network.SidebarS2CPayload;
import moe.mchdf.quake.network.SidebarS2CPayload.Row;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * 화면 오른쪽, 바닐라 스코어보드 자리에 서버가 보낸 사이드바를 반투명 패널로 그립니다.
 *
 * <p>서버는 모드 사용자에게 바닐라 사이드바를 보내지 않고 같은 내용을 {@link SidebarS2CPayload}로 보냅니다.
 * 문구와 색은 서버가 정하고, 이 클래스는 줄의 형태에 맞춰 꾸미기만 합니다.
 * <ul>
 *   <li>값 줄: 이름은 왼쪽, 값은 오른쪽 끝</li>
 *   <li>막대 줄: 값 아래에 진행 막대. 값이 바뀌면 막대가 부드럽게 따라갑니다.</li>
 *   <li>구분선: 얇은 가로선</li>
 *   <li>강조색: 패널 위쪽 선 (게임 중에는 내 팀 색)</li>
 * </ul>
 * HUD라서 클릭할 수 없고 게임 조작을 막지 않습니다.
 */
public final class SidebarHud {
	private static final int HEADER_HEIGHT = 16;
	private static final int VALUE_HEIGHT = 11;
	private static final int BAR_HEIGHT = 3;
	private static final int BAR_ROW_HEIGHT = VALUE_HEIGHT + BAR_HEIGHT + 3;
	private static final int SPACER_HEIGHT = 7;
	private static final int LABEL_VALUE_GAP = 12;
	/** 막대가 목표 값을 따라가는 빠르기. 클수록 빠릅니다. */
	private static final float BAR_SPEED = 8f;

	private sealed interface Line {
		record Spacer() implements Line {
		}

		record Text(Component text, boolean centered) implements Line {
		}

		record Value(Component label, Component value) implements Line {
		}

		record Bar(Component label, Component value, float ratio, int color) implements Line {
		}
	}

	private static boolean visible;
	private static Component title = Component.empty();
	private static int accent = QuakeTheme.ACCENT;
	private static List<Line> lines = List.of();
	/** 줄마다 화면에 보이는 막대 비율. 목표 값을 향해 조금씩 움직입니다. */
	private static float[] shownRatios = new float[0];
	private static long lastFrameNanos;

	private SidebarHud() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SidebarS2CPayload.TYPE,
				(payload, context) -> update(payload, context.client()));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(SidebarHud::clear));
		HudElementRegistry.attachElementAfter(VanillaHudElements.SCOREBOARD, QuakeMod.id("sidebar"), SidebarHud::extract);
	}

	private static void update(SidebarS2CPayload payload, Minecraft client) {
		if (!payload.visible()) {
			clear();
			return;
		}
		title = parse(payload.title(), client);
		accent = payload.accent() == 0 ? QuakeTheme.ACCENT : payload.accent();
		List<Line> parsed = new ArrayList<>(payload.rows().size());
		for (Row row : payload.rows()) {
			parsed.add(switch (row) {
				case Row.Spacer spacer -> new Line.Spacer();
				case Row.Text text -> new Line.Text(parse(text.text(), client), text.centered());
				case Row.Value value -> new Line.Value(parse(value.label(), client), parse(value.value(), client));
				case Row.Bar bar -> new Line.Bar(parse(bar.label(), client), parse(bar.value(), client),
						bar.ratio(), bar.color());
			});
		}
		boolean sameLayout = visible && sameShape(lines, parsed);
		lines = List.copyOf(parsed);
		if (!sameLayout) {
			// 처음 보이거나 줄 구성이 바뀌면 막대를 바로 목표 값으로 맞춥니다.
			shownRatios = new float[lines.size()];
			for (int i = 0; i < lines.size(); i++) {
				if (lines.get(i) instanceof Line.Bar bar) {
					shownRatios[i] = bar.ratio();
				}
			}
		}
		visible = true;
	}

	private static boolean sameShape(List<Line> before, List<Line> after) {
		if (before.size() != after.size()) {
			return false;
		}
		for (int i = 0; i < before.size(); i++) {
			if (before.get(i).getClass() != after.get(i).getClass()) {
				return false;
			}
		}
		return true;
	}

	private static void clear() {
		visible = false;
		title = Component.empty();
		accent = QuakeTheme.ACCENT;
		lines = List.of();
		shownRatios = new float[0];
	}

	/** 서버가 보낸 텍스트 컴포넌트 JSON을 읽습니다. 읽지 못하면 빈 글자로 둡니다. */
	private static Component parse(String json, Minecraft client) {
		DynamicOps<JsonElement> ops = client.getConnection() != null
				? client.getConnection().registryAccess().createSerializationContext(JsonOps.INSTANCE)
				: JsonOps.INSTANCE;
		try {
			return ComponentSerialization.CODEC.parse(ops, JsonParser.parseString(json))
					.resultOrPartial(error -> QuakeMod.LOGGER.warn("사이드바 글자를 읽지 못했습니다: {}", error))
					.orElse(Component.empty());
		} catch (JsonParseException e) {
			QuakeMod.LOGGER.warn("사이드바 글자가 올바른 JSON이 아닙니다: {}", json);
			return Component.empty();
		}
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!visible || !QuakeServerConnection.isActive()) {
			return;
		}
		animateBars();
		Font font = Minecraft.getInstance().font;
		int pad = QuakeTheme.HUD_PADDING;

		int width = Math.max(QuakeTheme.HUD_MIN_WIDTH, font.width(title) + pad * 2);
		int bodyHeight = pad;
		for (Line line : lines) {
			width = Math.max(width, contentWidth(font, line) + pad * 2);
			bodyHeight += height(line);
		}
		int totalHeight = HEADER_HEIGHT + bodyHeight;

		// 바닐라 스코어보드와 같은 자리: 오른쪽 끝, 아랫변이 화면 가운데보다 높이의 1/3만큼 아래
		int right = graphics.guiWidth() - 4;
		int left = right - width;
		int bottom = graphics.guiHeight() / 2 + totalHeight / 3;
		int top = bottom - totalHeight;
		int headerBottom = top + HEADER_HEIGHT;

		// 패널: 그림자, 배경, 제목 줄, 테두리, 위쪽 강조선
		graphics.fill(left + 2, top + 2, right + 2, bottom + 2, QuakeTheme.HUD_SHADOW);
		graphics.fill(left, top, right, headerBottom, QuakeTheme.HUD_HEADER);
		graphics.fill(left, headerBottom, right, bottom, QuakeTheme.HUD_BACKGROUND);
		graphics.horizontalLine(left, right - 1, headerBottom, QuakeTheme.HUD_DIVIDER);
		graphics.outline(left, top, width, totalHeight, QuakeTheme.HUD_BORDER);
		graphics.fill(left, top, right, top + 2, accent);

		// 반투명 배경 위에서도 읽히도록 글자에 그림자를 넣습니다.
		int titleY = top + 2 + (HEADER_HEIGHT - 2 - font.lineHeight) / 2 + 1;
		graphics.text(font, title, left + (width - font.width(title)) / 2, titleY, QuakeTheme.TEXT, true);

		int innerLeft = left + pad;
		int innerRight = right - pad;
		int y = headerBottom + pad / 2 + 1;
		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			switch (line) {
				case Line.Spacer spacer ->
						graphics.horizontalLine(innerLeft, innerRight - 1, y + SPACER_HEIGHT / 2, QuakeTheme.HUD_DIVIDER);
				case Line.Text text -> {
					int x = text.centered() ? left + (width - font.width(text.text())) / 2 : innerLeft;
					graphics.text(font, text.text(), x, y + 1, QuakeTheme.TEXT, true);
				}
				case Line.Value value -> labelAndValue(graphics, font, value.label(), value.value(), innerLeft, innerRight, y);
				case Line.Bar bar -> {
					labelAndValue(graphics, font, bar.label(), bar.value(), innerLeft, innerRight, y);
					int barTop = y + VALUE_HEIGHT;
					int filled = Math.round((innerRight - innerLeft) * shownRatios[i]);
					graphics.fill(innerLeft, barTop, innerRight, barTop + BAR_HEIGHT, QuakeTheme.HUD_TRACK);
					if (filled > 0) {
						graphics.fill(innerLeft, barTop, innerLeft + filled, barTop + BAR_HEIGHT, bar.color());
					}
				}
			}
			y += height(line);
		}
	}

	/** 이름은 왼쪽, 값은 오른쪽 끝에 그립니다. 서버가 색을 정하지 않은 이름은 HUD 이름 색으로 그립니다. */
	private static void labelAndValue(GuiGraphicsExtractor graphics, Font font, Component label, Component value,
									  int left, int right, int y) {
		graphics.text(font, label, left, y + 1, QuakeTheme.HUD_LABEL, true);
		graphics.text(font, value, right - font.width(value), y + 1, QuakeTheme.TEXT, true);
	}

	private static void animateBars() {
		long now = System.nanoTime();
		float seconds = lastFrameNanos == 0 ? 0f : Math.min(0.25f, (now - lastFrameNanos) / 1_000_000_000f);
		lastFrameNanos = now;
		float step = 1f - (float) Math.exp(-BAR_SPEED * seconds);
		for (int i = 0; i < lines.size() && i < shownRatios.length; i++) {
			if (lines.get(i) instanceof Line.Bar bar) {
				shownRatios[i] += (bar.ratio() - shownRatios[i]) * step;
			}
		}
	}

	private static int contentWidth(Font font, Line line) {
		return switch (line) {
			case Line.Spacer spacer -> 0;
			case Line.Text text -> font.width(text.text());
			case Line.Value value -> font.width(value.label()) + LABEL_VALUE_GAP + font.width(value.value());
			case Line.Bar bar -> font.width(bar.label()) + LABEL_VALUE_GAP + font.width(bar.value());
		};
	}

	private static int height(Line line) {
		return switch (line) {
			case Line.Spacer spacer -> SPACER_HEIGHT;
			case Line.Text text -> VALUE_HEIGHT;
			case Line.Value value -> VALUE_HEIGHT;
			case Line.Bar bar -> BAR_ROW_HEIGHT;
		};
	}
}
