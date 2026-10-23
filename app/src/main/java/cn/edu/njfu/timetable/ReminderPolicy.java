package cn.edu.njfu.timetable;

import java.util.*;

/** Reminder identity includes the current target time; expiry is the class start. */
public final class ReminderPolicy {
  public static final String PREF = "reminderDeliveriesV2";
  public static final long ZERO_GRACE = 30000L;

  private ReminderPolicy() {}

  public static String key(CalendarEngine.Event e) {
    // A zero-minute alarm is allowed a short delivery callback delay at the start itself.
    long expires = e.start + (e.remind == e.start ? ZERO_GRACE : 0);
    return expires + ":" + e.start + ":" + e.remind + ":" + e.course.id;
  }

  public static Set<String> prune(Set<String> saved, long now) {
    Set<String> result = new HashSet<>();
    for (String value : saved) {
      try {
        String[] parts = value.split(":", 4);
        if (parts.length != 4 || parts[3].isEmpty()) continue;
        long expires = Long.parseLong(parts[0]),
            start = Long.parseLong(parts[1]),
            remind = Long.parseLong(parts[2]);
        if (remind > start || expires != start + (remind == start ? ZERO_GRACE : 0)) continue;
        if (expires > now) result.add(value);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return result;
  }

  public static long at(CalendarEngine.Event e, long now, long notBefore) {
    return Math.max(e.remind, Math.max(now, notBefore));
  }

  public static CalendarEngine.Event next(
      List<CalendarEngine.Event> events, Set<String> delivered, long now, long notBefore) {
    CalendarEngine.Event best = null;
    long earliest = Long.MAX_VALUE;
    for (CalendarEngine.Event e : events) {
      if (e.start <= now || delivered.contains(key(e))) continue;
      long when = at(e, now, notBefore);
      if (when > e.start || (when == e.start && e.remind != e.start)) continue;
      if (when < earliest) {
        earliest = when;
        best = e;
      }
    }
    return best;
  }

  public static List<CalendarEngine.Event> due(
      List<CalendarEngine.Event> events, Set<String> delivered, long now, long scheduledAt) {
    List<CalendarEngine.Event> due = new ArrayList<>();
    for (CalendarEngine.Event e : events) {
      if (e.remind > now || delivered.contains(key(e))) continue;
      boolean zeroAtStart =
          e.remind == e.start && scheduledAt == e.start && now < e.start + ZERO_GRACE;
      if (e.start > now || zeroAtStart) due.add(e);
    }
    return due;
  }
}
