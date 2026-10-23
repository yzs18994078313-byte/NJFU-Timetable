package cn.edu.njfu.timetable;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Locale;

/** Request date and semester are independent of the session-assigned timetable scheme. */
public final class CourseQuery {
  public static final String ORIGIN = "https://jwxt.njfu.edu.cn";
  public static final String PATH = "/jsxsd/framework/mainV_index_loadkb.htmlx";
  public final String rq, scheme, term;

  public CourseQuery(String rq, String scheme, String term) {
    if (scheme == null || scheme.trim().isEmpty() || scheme.length() > 256)
      throw new IllegalArgumentException("请先登录并等待学校分配作息方案");
    if (term == null
        || !term.matches("20\\d{2}-20\\d{2}-[12]")
        || Integer.parseInt(term.substring(5, 9)) != Integer.parseInt(term.substring(0, 4)) + 1)
      throw new IllegalArgumentException("学期格式须为 2026-2027-1 或 2026-2027-2");
    this.rq = "all".equals(rq) ? "all" : normalizeMonday(rq);
    this.scheme = scheme.trim();
    this.term = term;
  }

  public static String normalizeMonday(String date) {
    if (date == null || !date.matches("\\d{4}-\\d{1,2}-\\d{1,2}"))
      throw new IllegalArgumentException("请填写周一日期，如2026-8-31");
    String[] p = date.split("-");
    String padded =
        String.format(
            Locale.US,
            "%04d-%02d-%02d",
            Integer.parseInt(p[0]),
            Integer.parseInt(p[1]),
            Integer.parseInt(p[2]));
    Calendar c = CalendarEngine.date(padded);
    if (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
      throw new IllegalArgumentException("查询日期必须是该周的周一");
    return CalendarEngine.format(c.getTimeInMillis(), "yyyy-M-d");
  }

  public static String weekDate(String semesterMonday, int week) {
    if (week < 1 || week > 40) throw new IllegalArgumentException("教学周须为1至40");
    Calendar c = CalendarEngine.date(semesterMonday);
    if (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
      throw new IllegalArgumentException("学期起始日期必须是周一");
    c.add(Calendar.DATE, (week - 1) * 7);
    return CalendarEngine.format(c.getTimeInMillis(), "yyyy-M-d");
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public String url() {
    return ORIGIN
        + PATH
        + "?rq="
        + encode(rq)
        + "&sjmsValue="
        + encode(scheme)
        + "&xnxqid="
        + encode(term)
        + "&xswk=false";
  }
}
