package cn.edu.njfu.timetable;

import java.util.*;

/** Calendar dates and course occurrences always use the school's time zone. */
public final class CalendarDays {
  public static final long DAY = 86400000L;

  private CalendarDays() {}

  public static long day(long millis) {
    return CalendarEngine.date(CalendarEngine.format(millis, "yyyy-MM-dd")).getTimeInMillis();
  }

  public static long month(long millis) {
    Calendar c = CalendarEngine.calendar(day(millis));
    c.set(Calendar.DAY_OF_MONTH, 1);
    return c.getTimeInMillis();
  }

  public static long shiftMonth(long millis, int delta) {
    Calendar c = CalendarEngine.calendar(month(millis));
    c.add(Calendar.MONTH, delta);
    return c.getTimeInMillis();
  }

  public static long gridStart(long millis) {
    return CalendarEngine.date(CalendarEngine.mondayNow(month(millis))).getTimeInMillis();
  }

  public static int cells(long millis) {
    long start = gridStart(millis), next = shiftMonth(millis, 1);
    return (int) (((next - start) / DAY + 6) / 7 * 7);
  }

  public static List<Course> coursesOn(List<Course> courses, String monday, int total, long date) {
    int w = CalendarEngine.currentWeek(monday, date);
    List<Course> result = new ArrayList<>();
    if (w < 1 || w > total) return result;
    int day = (CalendarEngine.calendar(date).get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
    for (Course c : courses) if (c.day == day && c.weeks.contains(w)) result.add(c);
    Collections.sort(
        result,
        (a, b) ->
            a.first == b.first ? a.name.compareTo(b.name) : Integer.compare(a.first, b.first));
    return result;
  }
}
