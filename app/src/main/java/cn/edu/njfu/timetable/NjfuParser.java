package cn.edu.njfu.timetable;

import java.util.*;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

/** Parses mainV_index_loadkb's HTML fragment; never executes its scripts or image URLs. */
public final class NjfuParser {
  private NjfuParser() {}

  public static final class Result {
    public final List<Course> courses = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
  }

  private static String normalize(String s) {
    return s.replace('～', '~').replace('－', '-').replace('—', '-').trim();
  }

  private static String teacher(Element box) {
    for (Element scope = box; scope != null; scope = scope.parent()) {
      Element preceding = scope.previousElementSibling();
      if (preceding != null && preceding.hasClass("box")) {
        String t = teacherIn(preceding);
        if (!t.isEmpty()) return t;
      }
      if (scope.hasClass("box")) {
        String t = teacherIn(scope);
        if (!t.isEmpty()) return t;
      }
      if (scope.tagName().equals("td")) break;
    }
    return "";
  }

  private static String teacherIn(Element e) {
    for (Element p : e.select("p")) {
      boolean detail = false;
      for (Element a = p.parent(); a != null && a != e; a = a.parent())
        if (a.hasClass("item-box")) {
          detail = true;
          break;
        }
      if (!detail && p.text().matches("^教师[：:].*")) return p.text().replaceFirst("^教师[：:]\\s*", "");
    }
    return "";
  }

  private static int weekday(String text) {
    Matcher m = Pattern.compile("(?:星期|周)([一二三四五六日天])").matcher(text);
    return m.find() ? "一二三四五六日".indexOf(m.group(1).replace('天', '日')) + 1 : 0;
  }

  private static int[] period(String text) {
    String s = normalize(text).replaceAll("\\s+", "").replace("小节", "节");
    Matcher m = Pattern.compile("^(\\d{1,2}(?:[~,-]\\d{1,2})*)节$").matcher(s);
    if (!m.matches()) throw new IllegalArgumentException("无法识别节次：" + text);
    String[] nums = m.group(1).split("[~,-]");
    int first = Integer.parseInt(nums[0]), last = Integer.parseInt(nums[nums.length - 1]);
    if (first < 1 || last > 20 || last < first) throw new IllegalArgumentException("节次超出范围");
    if (nums.length > 2)
      for (int i = 1; i < nums.length; i++)
        if (Integer.parseInt(nums[i]) != Integer.parseInt(nums[i - 1]) + 1)
          throw new IllegalArgumentException("不连续节次需拆分处理：" + text);
    return new int[] {first, last};
  }

  public static Result parse(String html) {
    if (html == null || html.length() > 2 * 1024 * 1024)
      throw new IllegalArgumentException("课表响应为空或超过2MB");
    Document doc = Jsoup.parse(html);
    Result result = new Result();
    Set<String> seen = new HashSet<>();
    for (Element box : doc.select(".item-box")) {
      // Malformed nested markup in the university response can contain several detail groups.
      List<Element> children = box.children();
      for (int i = 0; i < children.size(); i++) {
        Element nameNode = children.get(i);
        if (!nameNode.tagName().equals("p")) continue;
        String name = nameNode.text().trim();
        if (name.isEmpty()) continue;
        Element group = new Element("div");
        for (int j = i + 1; j < children.size() && !children.get(j).tagName().equals("p"); j++)
          group.appendChild(children.get(j).clone());
        // Nested details are parsed independently; this prevents combining two meetings.
        group.select(".item-box").remove();
        try {
          Element periods = group.selectFirst(".tch-name");
          if (periods == null) throw new IllegalArgumentException("缺少节次");
          String periodText = "";
          for (Element span : periods.select("span"))
            if (span.text().contains("节")) periodText = span.text();
          int[] range = period(periodText);
          Element weekIcon = group.selectFirst("img[src*=item3]");
          if (weekIcon == null) throw new IllegalArgumentException("缺少周次");
          String weekText = weekIcon.parent().text();
          int day = weekday(weekText);
          if (day == 0) throw new IllegalArgumentException("缺少星期");
          String weekExpr = weekText.replaceFirst("(?:星期|周)[一二三四五六日天].*$", "").trim();
          Set<Integer> weeks = Weeks.parse(weekExpr);
          Element roomIcon = group.selectFirst("img[src*=item1]");
          String room = roomIcon == null ? "" : roomIcon.parent().text();
          String teacher = teacher(box);
          String key =
              name + "|" + day + "|" + range[0] + "|" + range[1] + "|" + weeks + "|" + room;
          if (seen.add(key)) {
            result.courses.add(
                new Course(null, name, teacher, room, day, range[0], range[1], weeks));
            if (teacher.contains("..") || teacher.contains("…"))
              result.warnings.add(name + "的教师姓名在接口中被截断，可手动补全。");
          }
        } catch (IllegalArgumentException e) {
          result.warnings.add(name + "：" + e.getMessage());
        }
      }
    }
    if (result.courses.size() > 500) throw new IllegalArgumentException("课程时段超过500个");
    if (result.courses.isEmpty())
      result.warnings.add(
          doc.select("input[type=password]").isEmpty()
              ? "响应中没有可识别课程，请确认已登录并已选择正确学期和作息方案。"
              : "登录已失效，请重新登录。");
    return result;
  }
}
