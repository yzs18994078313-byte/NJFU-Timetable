package cn.edu.njfu.timetable;

import java.util.*;
import java.util.regex.*;

public final class Weeks {
  private Weeks() {}

  /** Compact ranges remain valid input for parse(), including odd/even runs. */
  public static String format(Set<Integer> weeks) {
    List<Integer> values = new ArrayList<>(new TreeSet<>(weeks));
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < values.size(); ) {
      int end = i, step = 1;
      if (i + 1 < values.size()) {
        int diff = values.get(i + 1) - values.get(i);
        if (diff == 1 || diff == 2) {
          step = diff;
          while (end + 1 < values.size() && values.get(end + 1) - values.get(end) == step) end++;
        }
      }
      if (result.length() > 0) result.append(',');
      result.append(values.get(i));
      if (end > i) {
        result.append('-').append(values.get(end));
        if (step == 2) result.append(values.get(i) % 2 == 1 ? "单" : "双");
      }
      i = end + 1;
    }
    return result.toString();
  }

  /** Reject unsupported patterns instead of silently scheduling a wrong teaching week. */
  public static Set<Integer> parse(String text) {
    if (text == null) throw new IllegalArgumentException("请填写教学周");
    String s =
        text.replaceAll("\\s+", "")
            .replace('，', ',')
            .replace('、', ',')
            .replace('；', ',')
            .replace(';', ',')
            .replace('－', '-')
            .replace('—', '-')
            .replace('～', '-')
            .replace('~', '-');
    s =
        s.replace("单周", "单")
            .replace("双周", "双")
            .replace("周次", "")
            .replace("第", "")
            .replace("周", "")
            .replace('（', '(')
            .replace('）', ')')
            .replace("(", "")
            .replace(")", "")
            .replace("[", "")
            .replace("]", "");
    if (s.isEmpty()) throw new IllegalArgumentException("请填写周次，例如 1-16、1-16单、1-8,10-16");
    TreeSet<Integer> result = new TreeSet<>();
    for (String token : s.split(",", -1)) {
      Matcher m = Pattern.compile("(\\d{1,2})(?:-(\\d{1,2}))?([单双])?").matcher(token);
      if (!m.matches()) throw new IllegalArgumentException("无法识别教学周：" + token);
      int a = Integer.parseInt(m.group(1)),
          b = m.group(2) == null ? a : Integer.parseInt(m.group(2));
      if (a < 1 || b > 40 || a > b) throw new IllegalArgumentException("教学周须在1至40之间，区间不能倒序");
      String parity = m.group(3);
      for (int n = a; n <= b; n++)
        if (parity == null || ("单".equals(parity) ? n % 2 == 1 : n % 2 == 0)) result.add(n);
    }
    if (result.isEmpty()) throw new IllegalArgumentException("周次条件没有匹配的教学周");
    return result;
  }
}
