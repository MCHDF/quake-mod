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
 * <p>서버는 모드 사용자에게 바닐라 사이드바를 보내지 않고 같은 제목·줄을 {@link SidebarS2CPayload}로 보냅니다.
 * 줄의 색과 문구는 서버가 정하므로 이 클래스는 내용의 의미를 모르고 그리기만 합니다.
 * HUD라서 클릭할 수 없고 게임 조작을 막지 않습니다.
 */
public final class SidebarHud {
	private static boolean visible;
	private static Component title = Component.empty();
	private static List<Component> lines = List.of();

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
		List<Component> parsed = new ArrayList<>(payload.lines().size());
		for (String line : payload.lines()) {
			parsed.add(parse(line, client));
		}
		lines = List.copyOf(parsed);
		visible = true;
	}

	private static void clear() {
		visible = false;
		title = Component.empty();
		lines = List.of();
	}

	/** 서버가 보낸 텍스트 컴포넌트 JSON을 읽습니다. 읽지 못하면 빈 줄로 둡니다. */
	private static Component parse(String json, Minecraft client) {
		DynamicOps<JsonElement> ops = client.getConnection() != null
				? client.getConnection().registryAccess().createSerializationContext(JsonOps.INSTANCE)
				: JsonOps.INSTANCE;
		try {
			return ComponentSerialization.CODEC.parse(ops, JsonParser.parseString(json))
					.resultOrPartial(error -> QuakeMod.LOGGER.warn("사이드바 줄을 읽지 못했습니다: {}", error))
					.orElse(Component.empty());
		} catch (JsonParseException e) {
			QuakeMod.LOGGER.warn("사이드바 줄이 올바른 JSON이 아닙니다: {}", json);
			return Component.empty();
		}
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!visible || !QuakeServerConnection.isActive()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		int pad = QuakeTheme.HUD_PADDING;
		int lineHeight = font.lineHeight;

		int textWidth = font.width(title);
		for (Component line : lines) {
			textWidth = Math.max(textWidth, font.width(line));
		}
		int width = textWidth + pad * 2;
		int headerHeight = lineHeight + 5;
		int bodyHeight = lines.size() * lineHeight + pad;
		int totalHeight = headerHeight + bodyHeight;

		// 바닐라 스코어보드와 같은 자리: 오른쪽 끝, 화면 세로 가운데보다 조금 아래가 아랫변
		int right = graphics.guiWidth() - 2;
		int left = right - width;
		int bottom = graphics.guiHeight() / 2 + totalHeight / 3;
		int top = bottom - totalHeight;
		int headerBottom = top + headerHeight;

		graphics.fill(left, top, right, headerBottom, QuakeTheme.HUD_HEADER);
		graphics.fill(left, headerBottom, right, bottom, QuakeTheme.HUD_BACKGROUND);
		graphics.fill(left, top, right, top + 1, QuakeTheme.ACCENT);

		// 반투명 배경 위에서도 읽히도록 글자에 그림자를 넣습니다.
		graphics.text(font, title, left + (width - font.width(title)) / 2, top + 3, QuakeTheme.TEXT, true);
		int y = headerBottom + pad / 2;
		for (Component line : lines) {
			graphics.text(font, line, left + pad, y, QuakeTheme.TEXT, true);
			y += lineHeight;
		}
	}
}
