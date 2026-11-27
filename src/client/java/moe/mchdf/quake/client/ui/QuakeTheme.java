package moe.mchdf.quake.client.ui;

/**
 * 모드 GUI 공통 스타일 값입니다. 스타일 테스트 중에는 여기 값만 바꿔 가며 확인합니다.
 * 색은 ARGB(0xAARRGGBB)입니다.
 */
public final class QuakeTheme {
	// 배경
	public static final int OVERLAY = 0x80000000;          // 팝업 뒤 게임 화면을 어둡게 하는 막

	// 패널
	public static final int PANEL = 0xF0161A22;            // 본문 배경
	public static final int PANEL_HEADER = 0xF01F2530;     // 제목 줄 배경
	public static final int PANEL_BORDER = 0xFF2C3442;     // 테두리
	public static final int ACCENT = 0xFFE8702A;           // 강조색 (테두리 위쪽 선, 주 버튼)
	public static final int ACCENT_HOVER = 0xFFFF8A45;
	public static final int DIVIDER = 0xFF2C3442;

	// 글자
	public static final int TEXT = 0xFFE6E9EF;
	public static final int TEXT_MUTED = 0xFF8C95A6;
	public static final int TEXT_ON_ACCENT = 0xFF1A1205;
	public static final int SUCCESS = 0xFF5BD68A;
	public static final int DANGER = 0xFFEF5B5B;
	public static final int HIGHLIGHT = 0xFFFFC83D;       // 노랑 강조 (선택 표시, 미리보기 버튼)

	// 보조 버튼
	public static final int BUTTON = 0xFF252C38;
	public static final int BUTTON_HOVER = 0xFF323B4B;
	public static final int BUTTON_BORDER = 0xFF3A4456;

	// HUD (게임 화면 위에 상시 표시되므로 반투명하게)
	public static final int HUD_BACKGROUND = 0x8C161A22;   // 본문 배경 (55%)
	public static final int HUD_HEADER = 0xC01F2530;       // 제목 줄 배경 (75%)
	public static final int HUD_BORDER = 0x40FFFFFF;       // 테두리
	public static final int HUD_SHADOW = 0x40000000;       // 패널 그림자
	public static final int HUD_DIVIDER = 0x30FFFFFF;      // 구분선
	public static final int HUD_TRACK = 0x40FFFFFF;        // 진행 막대의 빈 부분
	public static final int HUD_LABEL = 0xFFC5CCD6;        // 항목 이름 (회색보다 밝게)
	public static final int HUD_PADDING = 6;
	public static final int HUD_MIN_WIDTH = 120;

	// 크기
	public static final int HEADER_HEIGHT = 20;
	public static final int PADDING = 10;
	public static final int BUTTON_HEIGHT = 18;

	/** 팝업이 열릴 때 커지면서 나타나는 시간(ms). 0이면 애니메이션 없음. */
	public static final long OPEN_ANIMATION_MS = 140;

	private QuakeTheme() {
	}

	/** 색의 알파 값에 0~1 배율을 곱합니다. 페이드 효과에 씁니다. */
	public static int fade(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.clamp(alpha, 0f, 1f));
		return (a << 24) | (argb & 0x00FFFFFF);
	}
}
