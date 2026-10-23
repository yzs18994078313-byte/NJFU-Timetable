package cn.edu.njfu.timetable;

import java.util.*;

/**
 * Editable clock times. Changing a start time recalculates following periods using duration and
 * break.
 */
public final class PeriodTimes {
  public final int[] starts, lengths;
  public final int gap;

  public PeriodTimes(int[] starts, int[] lengths, int gap) {
    if (starts.length < 1 || starts.length > 20 || starts.length != lengths.length)
      throw new IllegalArgumentException("每天须有1至20节课");
    if (gap < 0 || gap > 180) throw new IllegalArgumentException("课间休息须为0至180分钟");
    this.starts = starts.clone();
    this.lengths = lengths.clone();
    this.gap = gap;
  }

  public static PeriodTimes from(String text, int savedGap) {
    int[][] periods = CalendarEngine.periods(text);
    int[] starts = new int[periods.length], lengths = new int[periods.length];
    Map<Integer, Integer> gaps = new TreeMap<>();
    for (int i = 0; i < periods.length; i++) {
      starts[i] = periods[i][0];
      lengths[i] = periods[i][1] - periods[i][0];
      if (i > 0) {
        int gap = periods[i][0] - periods[i - 1][1];
        if (gap <= 180) gaps.put(gap, gaps.getOrDefault(gap, 0) + 1);
      }
    }
    int common = 10, count = 0;
    for (Map.Entry<Integer, Integer> e : gaps.entrySet())
      if (e.getValue() > count) {
        common = e.getKey();
        count = e.getValue();
      }
    return new PeriodTimes(starts, lengths, savedGap >= 0 && savedGap <= 180 ? savedGap : common);
  }

  public int duration() {
    return lengths[0];
  }

  public boolean uniform() {
    for (int n : lengths) if (n != duration()) return false;
    return true;
  }

  public int end(int i) {
    return starts[i] + lengths[i];
  }

  public PeriodTimes withStart(int index, int minute) {
    if (minute < 0 || minute > 1439) throw new IllegalArgumentException("开始时间须在当天内");
    if (index < 0 || index >= starts.length) throw new IllegalArgumentException("节次无效");
    int[] next = starts.clone();
    next[index] = minute;
    for (int i = index + 1; i < next.length; i++) next[i] = next[i - 1] + lengths[i - 1] + gap;
    PeriodTimes result = new PeriodTimes(next, lengths, gap);
    result.validate();
    return result;
  }

  public PeriodTimes withDuration(int minutes) {
    if (minutes < 1 || minutes > 180) throw new IllegalArgumentException("课程时长须为1至180分钟");
    int[] next = new int[lengths.length];
    Arrays.fill(next, minutes);
    return new PeriodTimes(starts, next, gap);
  }

  public PeriodTimes withGap(int minutes) {
    return new PeriodTimes(starts, lengths, minutes);
  }

  public PeriodTimes withCount(int count) {
    if (count < 1 || count > 20) throw new IllegalArgumentException("每天须有1至20节课");
    int[] s = Arrays.copyOf(starts, count), d = Arrays.copyOf(lengths, count);
    for (int i = starts.length; i < count; i++) {
      s[i] = s[i - 1] + d[i - 1] + gap;
      d[i] = duration();
    }
    PeriodTimes next = new PeriodTimes(s, d, gap);
    if (count > starts.length) next.validate();
    return next;
  }

  public PeriodTimes arranged() {
    int[] s = starts.clone(), d = new int[lengths.length];
    Arrays.fill(d, duration());
    for (int i = 1; i < s.length; i++) s[i] = s[i - 1] + duration() + gap;
    PeriodTimes next = new PeriodTimes(s, d, gap);
    next.validate();
    return next;
  }

  public void validate() {
    int previousEnd = -1;
    for (int i = 0; i < starts.length; i++) {
      if (starts[i] < 0 || starts[i] > 1439 || lengths[i] <= 0 || end(i) > 1439)
        throw new IllegalArgumentException("第" + (i + 1) + "节须在当天23:59前结束");
      if (starts[i] < previousEnd)
        throw new IllegalArgumentException("第" + i + "节与第" + (i + 1) + "节时间重叠，请调整开始时间");
      previousEnd = end(i);
    }
  }

  public String serialize() {
    validate();
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < starts.length; i++) {
      if (i > 0) text.append('\n');
      text.append(CalendarEngine.time(starts[i])).append('-').append(CalendarEngine.time(end(i)));
    }
    return text.toString();
  }
}
