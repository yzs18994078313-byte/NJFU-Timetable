package cn.edu.njfu.timetable;

import java.util.*;

/** Interval layout: simultaneous courses get separate lanes rather than covering each other. */
public final class CalendarLayout {
  public static final class Entry {
    public final Course course;
    public final int start, end;
    public int lane, lanes;

    Entry(Course c, int start, int end) {
      course = c;
      this.start = start;
      this.end = end;
    }
  }

  private CalendarLayout() {}

  public static List<Entry> arrange(List<Course> courses, int[][] periods) {
    List<Entry> result = new ArrayList<>();
    for (int day = 1; day <= 7; day++) {
      List<Entry> daily = new ArrayList<>();
      for (Course c : courses)
        if (c.day == day && c.first >= 1 && c.last <= periods.length)
          daily.add(new Entry(c, periods[c.first - 1][0], periods[c.last - 1][1]));
      Collections.sort(
          daily,
          (a, b) ->
              a.start == b.start
                  ? Integer.compare(a.end, b.end)
                  : Integer.compare(a.start, b.start));
      List<Entry> cluster = new ArrayList<>();
      List<Integer> laneEnds = new ArrayList<>();
      int end = -1;
      for (Entry e : daily) {
        if (e.start >= end && !cluster.isEmpty()) {
          for (Entry v : cluster) v.lanes = laneEnds.size();
          cluster.clear();
          laneEnds.clear();
        }
        int lane = 0;
        while (lane < laneEnds.size() && laneEnds.get(lane) > e.start) lane++;
        if (lane == laneEnds.size()) laneEnds.add(e.end);
        else laneEnds.set(lane, e.end);
        e.lane = lane;
        cluster.add(e);
        result.add(e);
        end = Math.max(cluster.size() == 1 ? -1 : end, e.end);
      }
      for (Entry v : cluster) v.lanes = laneEnds.size();
    }
    return result;
  }
}
