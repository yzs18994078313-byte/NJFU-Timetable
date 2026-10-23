package cn.edu.njfu.timetable;

import java.text.*;
import java.util.*;

public final class CalendarEngine {
  public static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Shanghai");

  private CalendarEngine() {}

  public static Calendar calendar(long millis) {
    Calendar c = Calendar.getInstance(ZONE);
    c.setTimeInMillis(millis);
    return c;
  }

  public static Calendar date(String day) {
    if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}"))
      throw new IllegalArgumentException("日期格式须为 yyyy-MM-dd");
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    f.setTimeZone(ZONE);
    f.setLenient(false);
    try {
      Calendar c = calendar(f.parse(day).getTime());
      return c;
    } catch (ParseException e) {
      throw new IllegalArgumentException("日期无效");
    }
  }

  public static String format(long millis, String pattern) {
    SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.CHINA);
    f.setTimeZone(ZONE);
    return f.format(new Date(millis));
  }

  public static String mondayNow(long now) {
    Calendar c = calendar(now);
    c.add(Calendar.DATE, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7));
    return format(c.getTimeInMillis(), "yyyy-MM-dd");
  }

  public static int currentWeek(String monday, long now) {
    Calendar n = calendar(now);
    n.set(Calendar.HOUR_OF_DAY, 0);
    n.set(Calendar.MINUTE, 0);
    n.set(Calendar.SECOND, 0);
    n.set(Calendar.MILLISECOND, 0);
    return (int) Math.floor((n.getTimeInMillis() - date(monday).getTimeInMillis()) / 604800000d)
        + 1;
  }

  public static int minute(String s) {
    if (s == null || !s.matches("(?:[01]\\d|2[0-3]):[0-5]\\d"))
      throw new IllegalArgumentException("时间须为 HH:mm");
    return Integer.parseInt(s.substring(0, 2)) * 60 + Integer.parseInt(s.substring(3));
  }

  public static String time(int m) {
    return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
  }

  public static int[][] periods(String text) {
    String[] lines = text.trim().split("\\r?\\n");
    if (lines.length < 1 || lines.length > 20) throw new IllegalArgumentException("节次须为1至20节");
    int[][] result = new int[lines.length][2];
    int end = -1;
    for (int i = 0; i < lines.length; i++) {
      String[] t = lines[i].trim().split("-", -1);
      if (t.length != 2) throw new IllegalArgumentException("第" + (i + 1) + "行应为 HH:mm-HH:mm");
      int a = minute(t[0].trim()), b = minute(t[1].trim());
      if (a >= b || a < end) throw new IllegalArgumentException("节次时间必须按顺序排列且不能重叠");
      result[i][0] = a;
      result[i][1] = b;
      end = b;
    }
    return result;
  }

  public static long start(String monday, int week, int day, int minutes) {
    Calendar c = date(monday);
    c.add(Calendar.DATE, (week - 1) * 7 + day - 1);
    c.set(Calendar.HOUR_OF_DAY, minutes / 60);
    c.set(Calendar.MINUTE, minutes % 60);
    return c.getTimeInMillis();
  }

  public static final class Event {
    public final Course course;
    public final long start, remind;

    Event(Course c, long start, long remind) {
      this.course = c;
      this.start = start;
      this.remind = remind;
    }

    public String key() {
      return course.id + ":" + start;
    }
  }

  /**
   * An occurrence remains eligible until class starts, even if its reminder time is already due.
   */
  public static List<Event> events(
      List<Course> courses, String monday, int total, int[][] periods, int lead, long after) {
    List<Event> events = new ArrayList<>();
    for (Course c : courses) {
      if (c.last > periods.length) continue;
      for (int week : c.weeks) {
        if (week > total) continue;
        long start = start(monday, week, c.day, periods[c.first - 1][0]);
        long alarm = start - lead * 60000L;
        if (start > after) events.add(new Event(c, start, alarm));
      }
    }
    Collections.sort(events, (a, b) -> Long.compare(a.remind, b.remind));
    return events;
  }
}
