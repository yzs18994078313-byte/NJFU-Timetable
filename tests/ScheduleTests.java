package cn.edu.njfu.timetable;

import java.util.*;

public final class ScheduleTests {
  private static int checks;

  private static void ok(boolean test, String label) {
    checks++;
    if (!test) throw new AssertionError(label);
  }

  private static void rejects(Runnable run, String label) {
    boolean bad = false;
    try {
      run.run();
    } catch (IllegalArgumentException e) {
      bad = true;
    }
    ok(bad, label);
  }

  private static long date(String s) {
    return CalendarEngine.date(s).getTimeInMillis();
  }

  public static void main(String[] args) {
    String old = "08:00-08:45\n08:55-09:40\n10:00-10:45\n13:30-14:15";
    PeriodTimes p = PeriodTimes.from(old, -1);
    ok(p.serialize().equals(old), "old settings survive migration, including lunch");
    ok(p.gap == 10 && p.duration() == 45, "common gap inferred without treating lunch as default");
    ok(PeriodTimes.from(old, 15).gap == 15, "explicit saved gap respected");
    PeriodTimes changed = p.withStart(1, 540);
    ok(
        changed.starts[0] == 480
            && changed.starts[1] == 540
            && changed.starts[2] == 595
            && changed.starts[3] == 650,
        "middle edit preserves earlier period and recalculates all later starts");
    ok(changed.end(1) == 585, "duration follows edited start");
    ok(p.starts[1] == 535, "draft edit does not mutate saved source");
    ok(changed.serialize().contains("09:00-09:45"), "edited and recalculated values roundtrip");
    PeriodTimes first = p.withStart(0, 490);
    ok(
        Arrays.equals(first.starts, new int[] {490, 545, 600, 655}),
        "first edit propagates duration plus break through every later period");
    ok(first.end(3) == 700, "tail end follows recalculated start");
    PeriodTimes middle = p.withStart(2, 620);
    ok(
        Arrays.equals(middle.starts, new int[] {480, 535, 620, 675}),
        "earlier custom starts stay unchanged when editing third period");
    ok(
        Arrays.equals(p.withStart(3, 830).starts, new int[] {480, 535, 600, 830}),
        "last period can still be edited individually");
    ok(
        p.withGap(15).withStart(1, 540).starts[2] == 600,
        "automatic tail uses current configured break");
    ok(
        p.withStart(0, 480).starts[2] == 590,
        "confirming an unchanged anchor also recalculates later periods");
    rejects(
        () -> p.withStart(1, 1320),
        "late anchor that pushes a following period past midnight is rejected immediately");
    ok(p.serialize().equals(old), "rejected cascade leaves original schedule intact");
    PeriodTimes afternoon = first.withStart(2, 810);
    ok(
        afternoon.starts[0] == 490 && afternoon.starts[1] == 545 && afternoon.starts[3] == 865,
        "setting afternoon anchor preserves morning and recalculates afternoon tail");
    ok(p.withGap(15).starts[3] == 810, "gap edit preserves custom afternoon start");
    PeriodTimes arranged = p.withGap(15).arranged();
    ok(
        arranged.starts[1] == 540 && arranged.starts[3] == 660,
        "explicit bulk arrange uses duration and gap");
    ok(arranged.end(3) == 705, "bulk arrange end time");
    ok(
        p.withDuration(40).starts[3] == 810 && p.withDuration(40).end(3) == 850,
        "duration edit preserves independent starts");
    rejects(() -> p.withDuration(60).serialize(), "duration overlap blocks save");
    rejects(() -> p.withStart(1, 510).serialize(), "start overlap blocks save");
    rejects(() -> p.withStart(3, 1430).serialize(), "end outside current day blocks save");
    rejects(() -> p.withStart(0, -1), "negative time rejected");
    rejects(() -> p.withDuration(0), "zero duration rejected");
    rejects(() -> p.withGap(181), "excessive gap rejected");
    rejects(() -> p.withCount(0), "zero periods rejected");
    rejects(() -> p.withCount(21), "more than twenty periods rejected");
    ok(
        p.withCount(5).starts[4] == 865,
        "new period starts after previous end and configured break");
    ok(
        p.withCount(2).serialize().equals("08:00-08:45\n08:55-09:40"),
        "reducing count preserves retained values");
    rejects(() -> p.withCount(20), "appending across midnight rejected");
    PeriodTimes mixed = PeriodTimes.from("08:00-08:40\n09:00-09:50", -1);
    ok(
        !mixed.uniform() && mixed.serialize().endsWith("09:00-09:50"),
        "legacy mixed durations preserved until changed");
    ok(mixed.withDuration(35).uniform(), "explicit duration applies uniformly");
    ok(
        mixed.withGap(10).withStart(0, 500).starts[1] == 550,
        "cascade respects retained per-period duration from older settings");
    PeriodTimes edge = PeriodTimes.from("23:00-23:45", 0);
    rejects(() -> edge.withCount(2), "appending late-night period rejected");
    ok(CalendarDays.cells(date("2021-02-01")) == 28, "four-week February");
    ok(CalendarDays.cells(date("2026-08-01")) == 42, "six-week calendar");
    ok(
        CalendarDays.gridStart(date("2026-08-15")) == date("2026-07-27"),
        "Monday-first calendar with previous month");
    ok(
        CalendarDays.shiftMonth(date("2026-12-31"), 1) == date("2027-01-01"),
        "month navigation crosses year");
    Course odd = new Course("a", "Math", "", "A", 1, 1, 1, Weeks.parse("1-5单"));
    List<Course> courses = Arrays.asList(odd);
    String monday = "2026-08-31";
    ok(
        CalendarDays.coursesOn(courses, monday, 20, date("2026-08-31")).size() == 1,
        "first-week calendar occurrence");
    ok(
        CalendarDays.coursesOn(courses, monday, 20, date("2026-09-07")).isEmpty(),
        "even week hidden for odd-week course");
    ok(
        CalendarDays.coursesOn(courses, monday, 20, date("2026-09-14")).size() == 1,
        "next odd week appears");
    ok(
        CalendarDays.coursesOn(courses, monday, 20, date("2026-09-01")).isEmpty(),
        "weekday match required");
    ok(
        CalendarDays.coursesOn(courses, monday, 20, date("2026-08-24")).isEmpty(),
        "before semester empty");
    ok(
        CalendarDays.coursesOn(courses, monday, 1, date("2026-09-14")).isEmpty(),
        "after configured semester empty");
    int[][] times = CalendarEngine.periods(p.withStart(0, 470).serialize());
    List<CalendarEngine.Event> events = CalendarEngine.events(courses, monday, 20, times, 15, 0);
    ok(
        CalendarEngine.format(events.get(0).start, "HH:mm").equals("07:50"),
        "reminder uses independently edited start");
    ok(
        CalendarEngine.format(events.get(0).remind, "HH:mm").equals("07:35"),
        "lead subtracted from new start");
    Course later = new Course("tail", "Later", "", "", 1, 3, 3, Weeks.parse("1"));
    CalendarEngine.Event tail =
        CalendarEngine.events(
                Arrays.asList(later), monday, 20, CalendarEngine.periods(first.serialize()), 15, 0)
            .get(0);
    ok(
        CalendarEngine.format(tail.start, "HH:mm").equals("10:00")
            && CalendarEngine.format(tail.remind, "HH:mm").equals("09:45"),
        "later course reminder follows recalculated start");
    Course a = new Course("b", "Long", "", "", 1, 1, 2, Weeks.parse("1"));
    Course b = new Course("c", "Short", "", "", 1, 1, 1, Weeks.parse("1"));
    Course c = new Course("d", "Next", "", "", 1, 2, 3, Weeks.parse("1"));
    Course d = new Course("e", "Afternoon", "", "", 1, 4, 4, Weeks.parse("1"));
    Course otherDay = new Course("f", "Tuesday", "", "", 2, 1, 2, Weeks.parse("1"));
    List<CalendarLayout.Entry> layout =
        CalendarLayout.arrange(Arrays.asList(a, b, c, d, otherDay), CalendarEngine.periods(old));
    ok(layout.size() == 5, "all courses retained in layout");
    Map<String, CalendarLayout.Entry> map = new HashMap<>();
    for (CalendarLayout.Entry e : layout) map.put(e.course.id, e);
    ok(
        map.get("b").lanes == 2 && map.get("c").lane != map.get("b").lane,
        "overlapping courses split into lanes");
    ok(
        map.get("d").lanes == 2 && map.get("d").lane == map.get("c").lane,
        "lane reused after earlier course ends");
    ok(
        map.get("e").lanes == 1 && map.get("f").lanes == 1,
        "non-overlapping and different-day clusters get full width");
    ok(
        map.get("e").start == 810 && map.get("e").end == 855,
        "week calendar uses actual clock positions");
    System.out.println("Schedule/calendar checks passed: " + checks);
  }
}
