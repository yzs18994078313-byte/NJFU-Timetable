package cn.edu.njfu.timetable;

public final class QueryTests {
  private static int checks;

  private static void ok(boolean value, String name) {
    checks++;
    if (!value) throw new AssertionError(name);
  }

  private static void reject(Runnable r, String name) {
    boolean rejected = false;
    try {
      r.run();
    } catch (IllegalArgumentException e) {
      rejected = true;
    }
    ok(rejected, name);
  }

  public static void main(String[] args) {
    CourseQuery all = new CourseQuery("all", "session-assigned", "2026-2027-1");
    ok(all.url().contains("rq=all"), "full-semester query");
    CourseQuery week = new CourseQuery("2026-8-31", "scheme-A", "2026-2027-1");
    ok(week.url().contains("rq=2026-8-31"), "week uses actual first date, never week number");
    ok(
        week.url().contains("xnxqid=2026-2027-1") && week.url().endsWith("xswk=false"),
        "semester and online-course option retained");
    ok(
        new CourseQuery("2026-08-31", "scheme-B", "2026-2027-2")
            .url()
            .contains("sjmsValue=scheme-B&xnxqid=2026-2027-2"),
        "scheme and semester can change independently");
    ok(CourseQuery.weekDate("2026-08-31", 1).equals("2026-8-31"), "first teaching week date");
    ok(CourseQuery.weekDate("2026-08-31", 2).equals("2026-9-7"), "next week Monday");
    ok(CourseQuery.weekDate("2026-12-28", 2).equals("2027-1-4"), "cross-year week date");
    reject(() -> new CourseQuery("2026-8-30", "scheme", "2026-2027-1"), "Sunday rejected");
    reject(
        () -> new CourseQuery("2026-2-30", "scheme", "2026-2027-1"),
        "invalid calendar date rejected");
    reject(() -> new CourseQuery("all", "scheme", "2026-2028-1"), "invalid academic year rejected");
    reject(() -> new CourseQuery("all", "", "2026-2027-1"), "missing login scheme rejected");
    System.out.println("PASS: " + checks + " request parameter checks");
  }
}
