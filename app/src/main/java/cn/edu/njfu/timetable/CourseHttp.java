package cn.edu.njfu.timetable;

import java.io.*;
import java.net.*;

public final class CourseHttp {
  public static final String ORIGIN = CourseQuery.ORIGIN;
  public static final String PATH = CourseQuery.PATH;

  private CourseHttp() {}

  public static String fetch(String cookie, CourseQuery query, String userAgent, String referer)
      throws Exception {
    if (cookie == null || cookie.trim().isEmpty()) throw new IOException("尚未取得学校登录会话，请先登录");
    HttpURLConnection con = (HttpURLConnection) new URL(query.url()).openConnection();
    try {
      con.setConnectTimeout(20000);
      con.setReadTimeout(20000);
      con.setUseCaches(false);
      con.setInstanceFollowRedirects(false);
      con.setRequestMethod("GET");
      con.setRequestProperty("Cookie", cookie);
      con.setRequestProperty("User-Agent", userAgent);
      con.setRequestProperty(
          "Referer",
          referer != null && referer.startsWith(ORIGIN + "/jsxsd/")
              ? referer
              : ORIGIN + "/jsxsd/framework/xsMainV.jsp");
      con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
      con.setRequestProperty("Accept", "text/html, */*; q=0.01");
      int code = con.getResponseCode();
      if (code >= 300 && code < 400 || code == 401 || code == 403)
        throw new IOException("学校登录已失效或访问受限，请重新登录");
      if (code != 200) throw new IOException("教务系统响应 HTTP " + code);
      String type = con.getContentType(), charset = "UTF-8";
      if (type != null) {
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile(
                    "charset=[\\\"']?([\\w-]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(type);
        if (m.find()) charset = m.group(1);
      }
      return new String(Store.read(con.getInputStream()), charset);
    } finally {
      con.disconnect();
    }
  }
}
