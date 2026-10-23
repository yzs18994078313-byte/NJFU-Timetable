package cn.edu.njfu.timetable;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ParserTests {
  private static int checks;

  private static void ok(boolean b, String name) {
    checks++;
    if (!b) throw new AssertionError(name);
  }

  public static void main(String[] args) throws Exception {
    String html = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
    NjfuParser.Result r = NjfuParser.parse(html);
    for (Course c : r.courses)
      System.out.println(
          c.name
              + " | "
              + c.day
              + " | "
              + c.first
              + "-"
              + c.last
              + " | "
              + c.weekText()
              + " | "
              + c.teacher
              + " | "
              + c.room);
    ok(
        r.courses.size() == 16,
        "16 distinct recurring meetings; duplicates across big periods removed");
    ok(r.courses.stream().allMatch(c -> !c.name.endsWith("...")), "complete course names");
    ok(
        r.courses.stream()
            .anyMatch(
                c ->
                    c.day == 4 && c.first == 5 && c.last == 8 && c.weeks.equals(Weeks.parse("16"))),
        "week16 has four periods");
    ok(
        r.courses.stream()
            .anyMatch(
                c ->
                    c.day == 4
                        && c.first == 5
                        && c.last == 7
                        && c.weeks.equals(Weeks.parse("6-15"))),
        "weeks6-15 retain three periods");
    ok(
        r.courses.stream()
            .anyMatch(c -> c.day == 4 && c.first == 9 && c.weeks.equals(Weeks.parse("6-9,13-16"))),
        "discontinuous sports weeks");
    ok(
        r.courses.stream().anyMatch(c -> c.day == 2 && c.first == 9 && c.last == 11),
        "three consecutive small periods");
    ok(r.courses.stream().allMatch(c -> !c.room.isEmpty()), "locations retained");
    ok(r.courses.stream().allMatch(c -> !c.teacher.isEmpty()), "teacher labels retained");
    ok(!r.warnings.isEmpty(), "truncated teachers reported");
    ok(
        NjfuParser.parse("<form><input type=password value=secret></form>").courses.isEmpty(),
        "login HTML not imported");
    ok(
        NjfuParser.parse("<table><tr><td>暂无课程</td></tr></table>").courses.isEmpty(),
        "empty response handled");
    String fixture =
        "<span class='box'><p>教师：测试</p></span><div class='item-box'><p>测试课</p><div"
            + " class='tch-name'><span>01~02节</span></div><div><span><img"
            + " src='item1.png'>教室</span><span><img src='item3.png'>第1-16单周 星期一</span></div></div>";
    ok(
        NjfuParser.parse(fixture).courses.get(0).weeks.equals(Weeks.parse("1-16单")),
        "odd weeks parsed from response");
    ok(
        NjfuParser.parse(fixture.replace("01~02节", "01~03~05节")).courses.isEmpty(),
        "nonconsecutive section list never guesses a range");
    System.out.println("PASS: " + checks + " parser checks");
  }
}
