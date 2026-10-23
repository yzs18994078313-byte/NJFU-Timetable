package cn.edu.njfu.timetable;

/** Shared semantic colors; both palettes are checked for readable text contrast. */
public final class ThemeColors {
  public final boolean dark;
  public final int accent,
      primary,
      ink,
      muted,
      background,
      surface,
      soft,
      line,
      danger,
      outside,
      selection,
      today;
  public final int[] courses;

  public ThemeColors(boolean dark) {
    this.dark = dark;
    accent = dark ? 0xffa8c5ff : 0xff2e63d8;
    primary = dark ? 0xff365fcc : 0xff2e63d8;
    ink = dark ? 0xfff1f4fa : 0xff171a1f;
    muted = dark ? 0xffb1bac9 : 0xff626b7a;
    background = dark ? 0xff0d0f13 : 0xfff2f4f7;
    surface = dark ? 0xff191d24 : 0xffffffff;
    soft = dark ? 0xff243149 : 0xffe8efff;
    line = dark ? 0xff3b4351 : 0xffe4e7ec;
    danger = dark ? 0xffffaaa2 : 0xffa2362b;
    outside = dark ? 0xff98a4b7 : 0xff6d7787;
    selection = dark ? 0xff6684b5 : 0xff9ab7e8;
    today = dark ? 0x169cbaff : 0x0b2e63d8;
    courses =
        dark
            ? new int[] {0xffa8c8ff, 0xff9be5c0, 0xffffd18c, 0xffd8bfff, 0xffa0e6ef, 0xffffc2df}
            : new int[] {0xff2d5a9a, 0xff235f49, 0xff795015, 0xff674587, 0xff225f69, 0xff813e60};
  }
}
