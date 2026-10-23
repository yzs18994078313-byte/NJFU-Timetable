package cn.edu.njfu.timetable;

import java.util.*;

public final class CoreTests {
  private static int checks;

  private static void ok(boolean b, String message) {
    checks++;
    if (!b) throw new AssertionError(message);
  }

  private static void rejects(Runnable r, String label) {
    boolean failed = false;
    try {
      r.run();
    } catch (IllegalArgumentException e) {
      failed = true;
    }
    ok(failed, label);
  }

  public static void main(String[] args) {
    ok(
        Weeks.parse("1-16单").equals(new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9, 11, 13, 15))),
        "odd weeks");
    ok(
        Weeks.parse("第1-8周（双）").equals(new TreeSet<>(Arrays.asList(2, 4, 6, 8))),
        "Chinese even weeks");
    ok(
        Weeks.parse("1-3,5,8-10").equals(new TreeSet<>(Arrays.asList(1, 2, 3, 5, 8, 9, 10))),
        "discontinuous weeks");
    ok(
        Weeks.parse("1-4单,6-10双").equals(new TreeSet<>(Arrays.asList(1, 3, 6, 8, 10))),
        "independent parity patterns");
    ok(Weeks.parse("1—3、5，7").size() == 5, "Chinese punctuation");
    rejects(() -> Weeks.parse(""), "empty rejected");
    rejects(() -> Weeks.parse("0-4"), "zero rejected");
    rejects(() -> Weeks.parse("9-2"), "reversed rejected");
    rejects(() -> Weeks.parse("1-41"), "excess weeks rejected");
    rejects(() -> Weeks.parse("1,,3"), "empty segment rejected");
    rejects(() -> Weeks.parse("每周"), "ambiguous weeks rejected");
    rejects(() -> CalendarEngine.date("2026-02-30"), "invalid date rejected");
    rejects(() -> CalendarEngine.date("2026-1-1"), "date shape rejected");
    int[][] periods = CalendarEngine.periods("08:00-08:45\n08:55-09:40\n10:00-10:45");
    ok(periods[1][0] == 535, "period parsing");
    rejects(() -> CalendarEngine.periods("08:00-09:00\n08:30-09:30"), "overlap rejected");
    rejects(() -> CalendarEngine.minute("24:00"), "out of range time");
    String monday = "2026-09-07";
    long first = CalendarEngine.start(monday, 1, 1, 480);
    ok(
        CalendarEngine.format(first, "yyyy-MM-dd HH:mm").equals("2026-09-07 08:00"),
        "Shanghai first day");
    ok(
        CalendarEngine.format(CalendarEngine.start(monday, 2, 7, 600), "yyyy-MM-dd HH:mm")
            .equals("2026-09-20 10:00"),
        "week two Sunday");
    ok(CalendarEngine.currentWeek(monday, first - 9 * 3600000L) == 0, "before semester");
    ok(
        CalendarEngine.currentWeek(
                monday, CalendarEngine.date("2026-09-13").getTimeInMillis() + 23 * 3600000L)
            == 1,
        "Sunday is current week");
    ok(
        CalendarEngine.currentWeek(monday, CalendarEngine.date("2026-09-14").getTimeInMillis())
            == 2,
        "Monday advances week");
    Course odd = new Course("a", "Math", "Teacher", "Room", 1, 1, 2, Weeks.parse("1-5单"));
    List<CalendarEngine.Event> events =
        CalendarEngine.events(Arrays.asList(odd), monday, 4, periods, 15, first - 86400000L);
    ok(events.size() == 2, "odd weeks within semester only");
    ok(events.get(0).remind == first - 15 * 60000L, "adjustable lead");
    ok(
        CalendarEngine.events(Arrays.asList(odd), monday, 4, periods, 15, first - 15 * 60000L)
                .size()
            == 2,
        "occurrence remains eligible after reminder target until class starts");
    Course sunday = new Course("b", "Night", "", "", 7, 1, 1, Weeks.parse("1"));
    long sundayStart = CalendarEngine.start(monday, 1, 7, 480);
    ok(
        CalendarEngine.events(Arrays.asList(sunday), monday, 1, periods, 180, 0).get(0).remind
            == sundayStart - 180 * 60000L,
        "long lead correct");
    Course unconfigured = new Course("c", "Extra", "", "", 2, 4, 5, Weeks.parse("1"));
    ok(
        CalendarEngine.events(Arrays.asList(unconfigured), monday, 1, periods, 15, 0).isEmpty(),
        "missing period never guesses time");
    Course simultaneous = new Course("d", "Overlap", "", "", 1, 1, 1, Weeks.parse("1"));
    ok(
        CalendarEngine.events(Arrays.asList(odd, simultaneous), monday, 1, periods, 0, 0).size()
            == 2,
        "simultaneous courses both retained");
    TimeZone original = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
    ok(CalendarEngine.start(monday, 1, 1, 480) == first, "device timezone independence");
    TimeZone.setDefault(original);
    rejects(() -> new Course("x", "Invalid", "", "", 8, 1, 2, Weeks.parse("1")), "invalid day");
    rejects(() -> new Course("x", "Invalid", "", "", 1, 3, 2, Weeks.parse("1")), "invalid period");
    System.out.println("PASS: " + checks + " core checks");
  }
}
