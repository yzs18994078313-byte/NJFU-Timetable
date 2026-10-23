package cn.edu.njfu.timetable;

import java.util.*;

/**
 * End-to-end policy regressions for changed lead times, expiry, calendar axes and readable themes.
 */
public final class FeatureTests {
  private static int checks;

  private static void ok(boolean yes, String name) {
    checks++;
    if (!yes) throw new AssertionError(name);
  }

  private static CalendarEngine.Event event(Course c, long start, int lead) {
    return new CalendarEngine.Event(c, start, start - lead * 60000L);
  }

  private static double channel(int c) {
    double v = c / 255d;
    return v <= .04045 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4);
  }

  private static double lum(int c) {
    return .2126 * channel((c >> 16) & 255)
        + .7152 * channel((c >> 8) & 255)
        + .0722 * channel(c & 255);
  }

  private static double contrast(int a, int b) {
    return (Math.max(lum(a), lum(b)) + .05) / (Math.min(lum(a), lum(b)) + .05);
  }

  private static int tint(int color, int background, int alpha) {
    int out = 0xff000000;
    for (int shift : new int[] {0, 8, 16})
      out |=
          ((((color >> shift) & 255) * alpha + ((background >> shift) & 255) * (255 - alpha)) / 255)
              << shift;
    return out;
  }

  public static void main(String[] args) {
    Course course = new Course("id:with:colon", "Reminder", "", "Room", 1, 1, 1, Weeks.parse("1"));
    long start = CalendarEngine.start("2026-08-31", 1, 1, 600);
    long eight = start - 120 * 60000L;
    CalendarEngine.Event early = event(course, start, 180), later = event(course, start, 15);
    Set<String> ledger = new HashSet<>();
    ledger.add(ReminderPolicy.key(early));
    ok(
        ReminderPolicy.next(Arrays.asList(later), ledger, eight, 0) == later,
        "shorter lead rearms an occurrence already notified at a longer lead");
    ok(
        ReminderPolicy.at(later, eight, 0) == start - 15 * 60000L,
        "new reminder scheduled at new target time");
    ok(
        ReminderPolicy.due(Arrays.asList(later), ledger, eight, eight).isEmpty(),
        "shorter lead does not alert early");
    ok(
        ReminderPolicy.due(Arrays.asList(later), ledger, later.remind, later.remind).size() == 1,
        "new lead target sends again before class");
    ledger.add(ReminderPolicy.key(later));
    ok(
        ReminderPolicy.next(Arrays.asList(later), ledger, eight, 0) == null,
        "same target not scheduled twice");
    ok(
        ReminderPolicy.due(Arrays.asList(later), ledger, later.remind, later.remind).isEmpty(),
        "duplicate receiver does not repeat same target");
    ok(
        ReminderPolicy.next(Arrays.asList(early), ledger, eight, 0) == null,
        "returning to an already-delivered target does not repeat it");
    ok(
        ReminderPolicy.prune(ledger, start - 1).size() == 2,
        "delivery history remains until class begins");
    ok(ReminderPolicy.prune(ledger, start).isEmpty(), "history expires exactly at class start");
    long due = start - 5 * 60000L;
    Set<String> none = new HashSet<>();
    ok(
        ReminderPolicy.next(Arrays.asList(later), none, due, 0) == later,
        "unstarted course remains eligible after reminder target");
    ok(
        ReminderPolicy.at(later, due, 0) == due,
        "overdue target for upcoming class is scheduled immediately");
    ok(
        ReminderPolicy.due(Arrays.asList(later), none, due, due).size() == 1,
        "late callback can still notify before class");
    ok(
        ReminderPolicy.due(Arrays.asList(later), none, start, start).isEmpty(),
        "pre-class reminder expires at class start");
    ok(
        ReminderPolicy.next(Arrays.asList(later), none, start, 0) == null,
        "started class never rescheduled");
    ok(
        ReminderPolicy.due(Arrays.asList(later), none, start + 1000, start).isEmpty(),
        "old pre-class alarm cannot notify after class starts");
    CalendarEngine.Event zero = event(course, start, 0);
    ok(
        ReminderPolicy.next(Arrays.asList(zero), none, eight, 0) == zero,
        "zero-minute option schedules at class time");
    ok(
        ReminderPolicy.due(Arrays.asList(zero), none, start + 500, start).size() == 1,
        "scheduled zero-minute callback permits brief OS latency");
    ok(
        ReminderPolicy.due(Arrays.asList(zero), none, start + 500, start - 1).isEmpty(),
        "zero-minute grace cannot be used by another alarm");
    ok(
        ReminderPolicy.due(Arrays.asList(zero), none, start + ReminderPolicy.ZERO_GRACE + 1, start)
            .isEmpty(),
        "zero-minute callback grace bounded");
    ok(
        ReminderPolicy.due(Arrays.asList(zero), none, start + ReminderPolicy.ZERO_GRACE, start)
            .isEmpty(),
        "zero-minute grace expires at its exact deadline");
    Set<String> zeroSent = new HashSet<>(Arrays.asList(ReminderPolicy.key(zero)));
    ok(
        ReminderPolicy.due(
                Arrays.asList(zero),
                ReminderPolicy.prune(zeroSent, start + 500),
                start + 500,
                start)
            .isEmpty(),
        "zero-minute duplicate suppressed during callback grace");
    ok(
        ReminderPolicy.next(Arrays.asList(zero), none, start + 1, 0) == null,
        "opening app after start does not newly schedule zero-minute backlog");
    ok(
        ReminderPolicy.next(Arrays.asList(later), none, due, start + 1) == null,
        "failed notification retry cannot cross class expiry");
    ok(
        ReminderPolicy.at(later, due, due + 60000) == due + 60000,
        "notification failure retry is bounded away from busy loop");
    ok(
        ReminderPolicy.prune(new HashSet<>(Arrays.asList(course.id + ":" + start, "broken")), eight)
            .isEmpty(),
        "legacy course-only markers cannot block current target");
    CalendarEngine.Event nextWeek = event(course, start + 7 * CalendarDays.DAY, 15);
    ok(
        !ReminderPolicy.key(nextWeek).equals(ReminderPolicy.key(later)),
        "different week occurrences retain separate identities");
    Course other = new Course("other", "Other", "", "", 1, 1, 1, Weeks.parse("1"));
    ok(
        !ReminderPolicy.key(event(other, start, 15)).equals(ReminderPolicy.key(later)),
        "simultaneous courses retain separate identities");
    ok(
        ReminderPolicy.prune(new HashSet<>(Arrays.asList(ReminderPolicy.key(later))), eight)
            .contains(ReminderPolicy.key(later)),
        "course IDs containing colons survive ledger persistence");
    List<CalendarEngine.Event> active =
        CalendarEngine.events(
            Arrays.asList(course), "2026-08-31", 1, CalendarEngine.periods("10:00-10:45"), 15, due);
    ok(
        active.size() == 1 && active.get(0).start == start,
        "event generation uses start expiry instead of reminder expiry");
    ok(
        CalendarEngine.events(
                Arrays.asList(course),
                "2026-08-31",
                1,
                CalendarEngine.periods("10:00-10:45"),
                15,
                start)
            .isEmpty(),
        "event generation drops class at start");
    int[][] periods = CalendarEngine.periods("08:13-08:58\n09:08-09:53\n13:31-14:16");
    PeriodAxis axis = new PeriodAxis(periods, 52);
    ok(axis.position(493) == 0, "axis begins at configured non-hour start");
    ok(axis.position(538) == 52, "first boundary uses configured end");
    ok(axis.position(548) > axis.position(538), "configured break occupies real gap");
    ok(axis.position(811) > axis.position(593) + 100, "lunch remains a visible time gap");
    ok(
        axis.position(856) == axis.height(),
        "axis ends at configured last class end, not next whole hour");
    ok(
        axis.position(520) > 0 && axis.position(520) < 52,
        "in-class clock positions interpolate within exact boundaries");
    PeriodAxis adjacent = new PeriodAxis(CalendarEngine.periods("08:00-08:01\n08:01-08:02"), 52);
    ok(
        adjacent.position(481) == 52 && adjacent.height() == 104,
        "short adjacent periods retain readable labels without artificial break");
    for (String s :
        new String[] {"6-17", "1-15单", "2-16双", "1-8,10-16", "1,4,8", "1-3,5-9单,12-14"}) {
      Set<Integer> weeks = Weeks.parse(s);
      ok(Weeks.parse(Weeks.format(weeks)).equals(weeks), "compact range preserves weeks: " + s);
    }
    ok(
        Weeks.format(Weeks.parse("6-17")).equals("6-17"),
        "continuous teaching weeks displayed as range");
    ok(
        Weeks.format(Weeks.parse("1-15单")).equals("1-15单"),
        "odd-week range compressed without adding even weeks");
    for (boolean dark : new boolean[] {false, true}) {
      ThemeColors p = new ThemeColors(dark);
      String label = dark ? "dark" : "light";
      ok(contrast(p.ink, p.surface) >= 7, label + " main text contrast");
      ok(
          contrast(p.muted, p.surface) >= 4.5 && contrast(p.muted, p.background) >= 4.5,
          label + " secondary text contrast");
      ok(contrast(p.accent, p.soft) >= 4.5, label + " button label contrast");
      ok(contrast(0xffffffff, p.primary) >= 4.5, label + " primary button contrast");
      ok(contrast(p.danger, p.surface) >= 4.5, label + " error text contrast");
      ok(contrast(p.outside, p.surface) >= 4.5, label + " outside-month date contrast");
      for (int color : p.courses) {
        ok(contrast(color, tint(color, p.surface, 36)) >= 4.5, label + " week course contrast");
        ok(
            contrast(color, tint(color, p.soft, 20)) >= 4.5,
            label + " selected month course contrast");
      }
    }
    System.out.println("Feature checks passed: " + checks);
  }
}
