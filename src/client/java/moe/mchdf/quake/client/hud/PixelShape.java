package moe.mchdf.quake.client.hud;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * GUI 픽셀 단위로 찍는 작은 그림(조준점, 히트마커, 링). 좌표는 화면 가운데 픽셀을 (0, 0)으로 둡니다.
 *
 * <p>{@link #draw}는 모양 바깥에 한 픽셀 테두리를 먼저 깔고 그 위에 색을 칠합니다. 테두리는 겹치는 곳 없이
 * 한 번씩만 칠하므로 반투명이어도 얼룩지지 않습니다.
 */
final class PixelShape {
	/** 바닐라 조준점 그림 크기. 화면 가운데에 이 크기로 그려지므로 같은 중심 픽셀을 씁니다. */
	private static final int CROSSHAIR_SIZE = 15;

	private final Set<Long> pixels = new LinkedHashSet<>();
	private List<long[]> outline;

	static int centerX(GuiGraphicsExtractor graphics) {
		return (graphics.guiWidth() - CROSSHAIR_SIZE) / 2 + CROSSHAIR_SIZE / 2;
	}

	static int centerY(GuiGraphicsExtractor graphics) {
		return (graphics.guiHeight() - CROSSHAIR_SIZE) / 2 + CROSSHAIR_SIZE / 2;
	}

	PixelShape add(int x, int y) {
		pixels.add(key(x, y));
		outline = null;
		return this;
	}

	/** 가로선: x0부터 x1까지(포함) */
	PixelShape horizontal(int x0, int x1, int y) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			add(x, y);
		}
		return this;
	}

	/** 세로선: y0부터 y1까지(포함) */
	PixelShape vertical(int x, int y0, int y1) {
		for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
			add(x, y);
		}
		return this;
	}

	/** 반지름 {@code radius}인 원 둘레 */
	PixelShape circle(float radius) {
		int steps = Math.max(16, Math.round(radius * 8));
		for (int i = 0; i < steps; i++) {
			double angle = Math.PI * 2 * i / steps;
			add((int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius));
		}
		return this;
	}

	boolean isEmpty() {
		return pixels.isEmpty();
	}

	void draw(GuiGraphicsExtractor graphics, int centerX, int centerY, int color, int outlineColor) {
		if ((outlineColor >>> 24) != 0) {
			for (long[] p : outline()) {
				graphics.fill(centerX + (int) p[0], centerY + (int) p[1], centerX + (int) p[0] + 1,
						centerY + (int) p[1] + 1, outlineColor);
			}
		}
		for (long key : pixels) {
			int x = x(key);
			int y = y(key);
			graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
		}
	}

	private List<long[]> outline() {
		if (outline == null) {
			Set<Long> around = new LinkedHashSet<>();
			for (long key : pixels) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						long neighbor = key(x(key) + dx, y(key) + dy);
						if (!pixels.contains(neighbor)) {
							around.add(neighbor);
						}
					}
				}
			}
			outline = new ArrayList<>(around.size());
			for (long key : around) {
				outline.add(new long[] {x(key), y(key)});
			}
		}
		return outline;
	}

	private static long key(int x, int y) {
		return ((long) x << 32) | (y & 0xFFFFFFFFL);
	}

	private static int x(long key) {
		return (int) (key >> 32);
	}

	private static int y(long key) {
		return (int) key;
	}
}
