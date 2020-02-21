/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.logs;

import org.bedework.util.misc.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * User: mike Date: 1/14/20 Time: 22:20
 */
public class AccessLogEntry {
  protected String req;
  protected int curPos; // while parsing

  public String ip;

  public Long millis; // time converted to milliseconds
  public int hourOfDay;
  public String normDate; // yyyymmdd
  // date time
  public String dt;

  public String method;
  public String path;
  public String status;
  public long length;
  public String userAgent;

  String logName;

  public static AccessLogEntry fromString(final String req) {
    final AccessLogEntry ale = new AccessLogEntry();

    if (ale.parse(req) == null) {
      return null;
    }

    return ale;
  }

  /**
   * @param req log entry
   * @return position we reached or null for bad record
   */
  public Integer parse(final String req) {
    this.req = req;

    ip = req.substring(0, req.indexOf(" "));
    curPos = req.indexOf("[[");

    if (curPos < 0) {
      return null;
    }

    var end = req.indexOf(" ", curPos);
    dt = req.substring(curPos + 2, end);

    millis = millis();
    if (millis == null) {
      error("Unable to get millis for %s", req);
      return null;
    }

    curPos = end + 1;
    if (!passQuote()) {
      return null;
    }

    end = blank();
    if (end < 0) {
      return null;
    }

    method = req.substring(curPos, end);
    curPos = end + 1;

    end = req.indexOf(" HTTP/1.1\"", curPos);
    if (end < 0) {
      return null;
    }

    path = req.substring(curPos, end);

    curPos = end + 10;
    end = req.indexOf(" ", curPos);

    status = req.substring(curPos, end);

    curPos = end + 1;
    end = req.indexOf(" ", curPos);

    if (req.charAt(curPos) != '-') {
      length = Integer.parseInt(req.substring(curPos, end));
    }

    curPos = end + 1;
    quoted();  // ?

    userAgent = quoted();

    return curPos;
  }

  private boolean passQuote() {
    // Advance past quote
    while (req.charAt(curPos) != '"') {
      curPos++;
      if (!posValid()) {
        return false;
      }
    }

    curPos++;

    if (!posValid()) {
      return false;
    }

    return true;
  }

  private int blank() {
    // find blank
    return req.indexOf(" ", curPos);
  }

  private String quoted() {
    // Advance to quote
    while (req.charAt(curPos) != '"') {
      curPos++;
      if (!posValid()) {
        return null;
      }
    }

    var end = req.indexOf("\"", curPos + 1);
    if (end < 0) {
      return null;
    }

    var res = req.substring(curPos + 1, end - 1);
    curPos = end + 1;

    return res;
  }

  private static Map<String, String> toMonth = new HashMap<>();
  static {
    toMonth.put("Jan", "01");
    toMonth.put("Feb", "02");
    toMonth.put("Mar", "03");
    toMonth.put("Apr", "04");
    toMonth.put("May", "05");
    toMonth.put("Jun", "06");
    toMonth.put("Jul", "07");
    toMonth.put("Aug", "08");
    toMonth.put("Sep", "09");
    toMonth.put("Oct", "10");
    toMonth.put("Nov", "11");
    toMonth.put("Dec", "12");
  }

  public Long millis() {
    try {
      // 20/Feb/2020:00:23:19
      // 0123456789012345678901234

      hourOfDay = Integer.parseInt(dt.substring(12, 14));
      final long mins = Integer.parseInt(dt.substring(15, 17));
      final long secs = Integer.parseInt(dt.substring(19, 20));

      normDate = dt.substring(7, 11) + "/" +
              toMonth.get(dt.substring(3, 6)) + "/" +
              dt.substring(0, 2);

      return ((((hourOfDay * 60) + mins) * 60) + secs) * 1000;
    } catch (final Throwable ignored) {
      return null;
    }
  }

  public boolean is404() {
    return isStatus("404");
  }

  public boolean is500() {
    return isStatus("500");
  }

  public boolean isStatus(final String s) {
    return s.equals(status);
  }

  public boolean legacyFeeder() {
    if ((method == null) || (path == null)) {
      return false;
    }

    return "GET".equals(method) &&
            path.startsWith("/feeder/") &&
            (path.contains("?calPath=") || path.contains("&calPath="));
  }

  public boolean webCache() {
    if ((method == null) || (path == null)) {
      return false;
    }

    return "GET".equals(method) &&
            path.startsWith("/webcache/v1.0/");
  }

  protected boolean posValid() {
    return (curPos >= 0) && (curPos < req.length());
  }

  protected void error(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  protected void out(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  protected void toStringSegment(final ToString ts) {
    ts.append("ip", ip);
    ts.append("normDate", normDate);
    ts.append("millis", millis);
    ts.append("method", method);
  }

  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
