package cn.edu.njfu.timetable;

/** Layout boundaries come from actual periods, with room for legible labels for short classes. */
public final class PeriodAxis {
  private final int[][] periods;
  private final float[] top, bottom;

  public PeriodAxis(int[][] periods, float minimumHeight) {
    this.periods = periods;
    top = new float[periods.length];
    bottom = new float[periods.length];
    float y = 0;
    for (int i = 0; i < periods.length; i++) {
      top[i] = y;
      bottom[i] = y + Math.max(minimumHeight, (periods[i][1] - periods[i][0]) * 64f / 60f);
      y = bottom[i];
      if (i + 1 < periods.length) {
        int gap = periods[i + 1][0] - periods[i][1];
        if (gap > 0) y += Math.max(8, gap * 64f / 60f);
      }
    }
  }

  public float height() {
    return bottom[bottom.length - 1];
  }

  public float position(int minute) {
    if (minute <= periods[0][0]) return 0;
    for (int i = 0; i < periods.length; i++) {
      if (minute <= periods[i][1])
        return top[i]
            + (minute - periods[i][0]) * (bottom[i] - top[i]) / (periods[i][1] - periods[i][0]);
      if (i + 1 < periods.length && minute < periods[i + 1][0])
        return bottom[i]
            + (minute - periods[i][1])
                * (top[i + 1] - bottom[i])
                / (periods[i + 1][0] - periods[i][1]);
    }
    return height();
  }
}
