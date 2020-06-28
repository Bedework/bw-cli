/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwlogs.AccessDay;
import org.bedework.bwlogs.AccessLogEntry;
import org.bedework.bwlogs.AccessTracker;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/** Used to check access logs for useful info.
 *
 * User: mike Date: 3/12/19 Time: 13:40
 */
public class AccessLogs {
  int numLegacy;
  int numWebcache;

  int req404;
  int req500;
  int feederUnknown;
  int webCacheUnknown;

  public static AccessTracker accessTracker = new AccessTracker();

  public boolean analyze(final String logPathName) {
    try {
      final LineNumberReader lnr = getLnr(logPathName);

      while (true) {
        final String s = lnr.readLine();

        if (s == null) {
          break;
        }

        final AccessLogEntry ale;
        try {
          ale = AccessLogEntry.fromString(s);
        } catch (final Throwable t) {
          out("Unable to parse line at %s\n%s",
              lnr.getLineNumber(), s);
          return false;
        }

        if (ale == null) {
          continue;
        }

        accessTracker.updateFrom(ale);

        if (ale.is404()) {
          req404++;
          continue;
        }

        if (ale.is500()) {
          req500++;
          continue;
        }

        if (ale.legacyFeeder()) {
          doLegacyFeeder(ale);
          continue;
        }

        if (ale.webCache()) {
          doWebCache(ale);
          continue;
        }
      }

      results();

      return true;
    } catch (final Throwable t) {
      t.printStackTrace();
      return false;
    }
  }

  private LineNumberReader getLnr(final String logPathName) {
    try {
      final Path logPath = Paths.get(logPathName);

      final File logFile = logPath.toFile();

      return new LineNumberReader(new FileReader(logFile));
    } catch (final FileNotFoundException fnfe) {
      final var msg = "No such file: " + logPathName;
      out(msg);
      throw new RuntimeException(msg);
    }
  }

  private void results() {
    out("Requests getting a 404: %d", req404);
    out("Requests getting a 500: %d", req500);

    out("Total feeder legacy requests: %d", numLegacy);
    int pattern = 1;
    for (final FeedMatcher m: feedMatchers) {
      out("Total feeder pattern%d requests: %d", pattern, m.matched);
      pattern++;
    }
    out("Total unknown feeder requests: %d", feederUnknown);

    out();

    out("Total webcache requests: %d", numWebcache);
    for (final WebcacheMatcher m: webcacheMatchers) {
      out("Total webcache pattern%d requests: %d", pattern, m.matched);
      pattern++;
    }
    out("Total unknown webcache requests: %d", webCacheUnknown);

    for (final String day: accessTracker.getSortedKeys()) {
      final AccessDay dayVal = accessTracker.getDay(day);

      out();

      out1day(day, dayVal);
    }
  }

  private void out1day(final String day,
                       final AccessDay dayVal) {
    out("Ip counts for %s", day);
    out();

    final List<Map.Entry<String, Integer>> longSorted =
            dayVal.getSortedIpCounts();

    long total = 0L;

    for (final Map.Entry<String, Integer> ent: longSorted) {
      final int ct = ent.getValue();
      total += ct;
      outFmt("%-15s\t%d", ent.getKey(), ct);
    }

    out();
    out("Total: %s", total);

    out("Ip domain counts for %s", day);
    out();

    final List<Map.Entry<String, Integer>> long2Sorted =
            dayVal.getSortedIp2Counts();

    total = 0L;

    for (final Map.Entry<String, Integer> ent: long2Sorted) {
      final int ct = ent.getValue();
      total += ct;
      outFmt("%-15s\t%d", ent.getKey(), ct);
    }

    out();
    out("Total: %s", total);

    out("Avg requests per minute for each hour:");
    for (int i = 0; i <= 23; i++) {
      out("%2s: %.2f", i, dayVal.getHour(i).perSecond() * 60);
    }

    out("Avg requests per minute for day: %.2f", dayVal.perSecond() * 60);
  }

  private void outFmt(final String format,
                      final Object... args) {
    System.out.println(String.format(format, args));
  }

  private void doLegacyFeeder(final AccessLogEntry ale) {
    numLegacy++;

    final URI uri;
    try {
      uri = new URI(ale.path);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }

    final List<NameValuePair> params =
            URLEncodedUtils.parse(uri, "UTF-8");

    final Map<String, List<String>> paramsMap = new HashMap<>();

    for (final NameValuePair param : params) {
      paramsMap.computeIfAbsent(param.getName(),
                                k -> new ArrayList<>())
               .add(param.getValue());
    }

    for (final FeedMatcher m: feedMatchers) {
      if (m.match(ale.path, params, paramsMap)) {
        m.matched++;
        return;
      }
    }

    feederUnknown++;
    out("Not matched %s", ale.path);
  }

  private void doWebCache(final AccessLogEntry ale) {
    numWebcache++;

    /*
    Webcache path is:
        /webcache
        /v1.0
        /jsonDays  |  /rssDays
        /<int>   number of days
        /list-rss  |  /list-json    skin name
        /no--filter | /<fexpr>
        /bwObject.json
     */

    final URI uri;
    try {
      uri = new URI(ale.path);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }

    final List<String> ruri = fixPath(uri.getPath());

    for (final WebcacheMatcher m: webcacheMatchers) {
      if (m.match(ale.path, ruri)) {
        m.matched++;
        return;
      }
    }

    webCacheUnknown++;
    out("Not matched %s", ale.path);
  }

  private List<String> fixPath(final String path) {
    if (path == null) {
      return null;
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
    } catch (final Throwable t) {
      throw new RuntimeException("bad path: " + path);
    }

    if (decoded == null) {
      return (null);
    }

    /* Make any backslashes into forward slashes.
     */
    if (decoded.indexOf('\\') >= 0) {
      decoded = decoded.replace('\\', '/');
    }

    /* Ensure a leading '/'
     */
    if (!decoded.startsWith("/")) {
      decoded = "/" + decoded;
    }

    /* Remove all instances of '//'.
     */
    while (decoded.contains("//")) {
      decoded = decoded.replaceAll("//", "/");
    }

    /* Somewhere we may have /./ or /../
     */

    final StringTokenizer st = new StringTokenizer(decoded, "/");

    final ArrayList<String> al = new ArrayList<>();
    while (st.hasMoreTokens()) {
      final String s = st.nextToken();

      if (s.equals("..")) {
        // Back up 1
        if (al.size() == 0) {
          // back too far
          return null;
        }

        al.remove(al.size() - 1);
      } else if (!s.equals(".")) {
        al.add(s);
      }
    }

    return al;
  }

  /* Pattern 1 is of this form:
    /feeder/main/listEvents.do
       Request params:
         calPath=/public/cals/MainCal
         skinName=list-json
         setappvar=objName(bwObject)
         setappvar=summaryMode(details)
         fexpr=%28catuid%3D%272962aca4-289343b8-0128-98411c36-0000001e%27%29
         days=30
   */
  private static class FeedPattern1 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 6, paramsMap, 5)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1) ||
              !checkParam(paramsMap, "setappvar", 2) ||
              !checkParam(paramsMap, "fexpr", 1) ||
              !checkParam(paramsMap, "days", 1)) {
        return false;
      }

      if (!checkParVal(paramsMap.get("skinName").get(0),
                       "list-json")) {
        return false;
      }

      return onlyCatUids(paramsMap.get("fexpr").get(0));
    }
  }

  /* Pattern 2 is of this form:
    /feeder/main/listEvents.do
       Request params:
         calPath=/public/cals/MainCal
         skinName=list-rss | list-json
         setappvar=summaryMode(details)
         days=1
   */
  private static class FeedPattern2 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 4, paramsMap, 4)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "days", 1)) {
        return false;
      }

      return checkParVal(paramsMap.get("skinName").get(0),
                         "list-rss", "list-json", "default");
    }
  }

  /* Pattern 3 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=/public/cals/MainCal
        skinName=list-rss
        setappvar=summaryMode(details)
        fexpr=%28catuid%3D%27ff808181-1fd7389e-011f-d7389f4b-00000018%27%29
        days=20
   */
  private static class FeedPattern3 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 5, paramsMap, 5)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "fexpr", 1) ||
              !checkParam(paramsMap, "days", 1)) {
        return false;
      }

      if (!checkParVal(paramsMap.get("skinName").get(0),
                       "list-rss", "list-json", "default")) {
        return false;
      }

      return onlyCatUids(paramsMap.get("fexpr").get(0));
    }
  }

  /* Pattern 4 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=%2Fpublic%2Fcals%2FMainCal
        skinName=list-rss
   */
  private static class FeedPattern4 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 2, paramsMap, 2)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1)) {
        return false;
      }

      return checkParVal(paramsMap.get("skinName").get(0),
                         "list-rss");
    }
  }

  /* Pattern 5 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=%2Fpublic%2Fcals%2FMainCal
        skinName=list-json
        setappvar=summaryMode(details)
        setappvar=objName(bwObject)
        days=1
   */
  private static class FeedPattern5 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 5, paramsMap, 4)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1) ||
              !checkParam(paramsMap, "setappvar", 2) ||
              !checkParam(paramsMap, "days", 1)) {
        return false;
      }

      return checkParVal(paramsMap.get("skinName").get(0),
                         "list-json");
    }
  }

  /* Pattern 6 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=/public/cals/MainCal
        format=text/calendar
        setappvar=summaryMode(details)
        fexpr=%28catuid%3D%272962ac9d-4b262989-014b-26e8a64a-00007157%27%29
        days=7
   */
  private static class FeedPattern6 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 5, paramsMap, 5)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "format", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "fexpr", 1) ||
              !checkParam(paramsMap, "days", 1)) {
        return false;
      }

      return onlyCatUids(paramsMap.get("fexpr").get(0));
    }
  }

  /* Pattern 7 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=/public/cals/MainCal
        format=text/calendar
        setappvar=summaryMode(details)
        days=7
   */
  private static class FeedPattern7 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 4, paramsMap, 4)) {
        return false;
      }

      return checkParam(paramsMap, "calPath", 1) &&
              checkParam(paramsMap, "format", 1) &&
              checkParam(paramsMap, "setappvar", 1) &&
              checkParam(paramsMap, "days", 1);
    }
  }

  /* Pattern 8 is of this form:
    /feeder/widget/categories.do
       Request params:
        skinName=widget-json-cats
        setappvar=objName(catsObj)
        calPath=/public/cals/MainCal
   */
  private static class FeedPattern8 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/widget/categories.do",
                 params, 3, paramsMap, 3)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "skinName", 1)) {
        return false;
      }

      return checkParVal(paramsMap.get("skinName").get(0),
                       "widget-json-cats");
    }
  }

  /* Pattern 9 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=/public/cals/MainCal
        skinName=list-rss
        setappvar=summaryMode(details)
        fexpr=%28catuid%3D%272962aca4-289343b8-0128-942028ce-00000005%27%7ccatuid%3D%27ff808181-1fd73b03-011f-d73b0642-00000001%27%7ccatuid%3D%27ff808181-1fd7389e-011f-d7389ed0-00000002%27%7ccatuid%3D%272962aca4-289343b8-0128-9423162b-0000000b%27%7ccatuid%3D%272962aca4-289343b8-0128-9424f694-0000000e%27%7ccatuid%3D%272962ac9d-29fa2ae9-0129-ff73a976-0000047c%27%7ccatuid%3D%272962aca4-289343b8-0128-9420505d-00000006%27%7ccatuid%3D%27ff808181-1fd73b03-011f-d73b065c-00000002%27%29
        start=2019-03-04
        end=2020-03-04
   */
  private static class FeedPattern9 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 6, paramsMap, 6)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "fexpr", 1) ||
              !checkParam(paramsMap, "start", 1) ||
              !checkParam(paramsMap, "end", 1)) {
        return false;
      }

      if (!checkParVal(paramsMap.get("skinName").get(0),
                       "list-rss")) {
        return false;
      }

      return onlyCatUids(paramsMap.get("fexpr").get(0));
    }
  }

  /* Pattern 10 is of this form:
    /feeder/main/listEvents.do
       Request params:
        calPath=/public/cals/MainCal
        skinName=list-rss
        setappvar=summaryMode(details)
        start=2019-03-04
        end=2020-03-04
   */
  private static class FeedPattern10 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 5, paramsMap, 5)) {
        return false;
      }

      //noinspection SimplifiableIfStatement
      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "skinName", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "start", 1) ||
              !checkParam(paramsMap, "end", 1)) {
        return false;
      }

      return checkParVal(paramsMap.get("skinName").get(0),
                       "list-rss");
    }
  }

  private final static FeedMatcher[] feedMatchers = {
          new FeedPattern1(),
          new FeedPattern2(),
          new FeedPattern3(),
          new FeedPattern4(),
          new FeedPattern5(),
          new FeedPattern6(),
          new FeedPattern7(),
          new FeedPattern8(),
          new FeedPattern9(),
          new FeedPattern10(),
          };

  abstract static class FeedMatcher {
    int matched;

    abstract boolean match(String urlStr,
                           List<NameValuePair> params,
                           Map<String, List<String>> paramsMap);

    boolean check(final String urlStr,
                  final String expectedUrlStr,
                  final List<NameValuePair> params,
                  final int paramsSize,
                  final Map<String, List<String>> paramsMap,
                  final int mapSize) {
      if (!urlStr.startsWith(expectedUrlStr)) {
        return false;
      }

      //noinspection SimplifiableIfStatement
      if (params.size() != paramsSize) {
        return false;
      }

      return paramsMap.size() == mapSize;
    }

    boolean checkParVal(final String val,
                        final String... vals) {
      if (val == null) {
        return false;
      }

      for (final String possible : vals) {
        if (val.equals(possible)) {
          return true;
        }
      }

      return false;
    }

    boolean checkParam(final Map<String, List<String>> paramsMap,
                       final String name,
                       final int num) {
      final List<String> vals = paramsMap.get(name);

      return (vals != null) && (vals.size() == num);
    }
  }

  private static class WebcachePattern1 extends WebcacheMatcher {
    boolean match(final String urlStr,
                  final List<String> ruri) {
      if (ruri.size() != 7) {
        return false;
      }

      if (!"webcache".equals(ruri.get(0))) {
        return false;
      }

      if (!"v1.0".equals(ruri.get(1))) {
        return false;
      }

      if (!"jsonDays".equals(ruri.get(2)) &&
              !"rssDays".equals(ruri.get(2))) {
        return false;
      }

      if (!isInt(ruri.get(3))) {
        return false;
      }

      if (!"list-rss".equals(ruri.get(4)) &&
              !"list-json".equals(ruri.get(4))) {
        return false;
      }

      final String fexpr = ruri.get(5);

      if (!"no--filter".equals(fexpr) &&
              !onlyCatUids(fexpr)) {
        return false;
      }

      return "bwObject.json".equals(ruri.get(6)) ||
              "no--object.json".equals(ruri.get(6));
    }
  }

  private static class WebcachePattern2 extends WebcacheMatcher {
    boolean match(final String urlStr,
                  final List<String> ruri) {
      if (ruri.size() != 6) {
        return false;
      }

      if (!"webcache".equals(ruri.get(0))) {
        return false;
      }

      if (!"v1.0".equals(ruri.get(1))) {
        return false;
      }

      if (!"jsonDays".equals(ruri.get(2)) &&
              !"rssDays".equals(ruri.get(2)) &&
              !"xmlDays".equals(ruri.get(2))) {
        return false;
      }

      if (!isInt(ruri.get(3))) {
        return false;
      }

      if (!"list-rss".equals(ruri.get(4)) &&
              !"list-json".equals(ruri.get(4)) &&
              !"list-xml".equals(ruri.get(4))) {
        return false;
      }

      final String fexpr = ruri.get(5);

      return "no--filter.rss".equals(fexpr) ||
              "no--filter.xml".equals(fexpr) ||
              onlyCatUids(fexpr);
    }
  }

  private static class WebcachePattern3 extends WebcacheMatcher {
    boolean match(final String urlStr,
                  final List<String> ruri) {
      if (ruri.size() != 5) {
        return false;
      }

      if (!"webcache".equals(ruri.get(0))) {
        return false;
      }

      if (!"v1.0".equals(ruri.get(1))) {
        return false;
      }

      if (!"icsDays".equals(ruri.get(2))) {
        return false;
      }

      if (!isInt(ruri.get(3))) {
        return false;
      }

      final String fexpr = ruri.get(4);

      return "no--filter.ics".equals(fexpr) ||
              onlyCatUids(fexpr);
    }
  }

  private static class WebcachePattern4 extends WebcacheMatcher {
    boolean match(final String urlStr,
                  final List<String> ruri) {
      if (ruri.size() != 5) {
        return false;
      }

      if (!"webcache".equals(ruri.get(0))) {
        return false;
      }

      if (!"v1.0".equals(ruri.get(1))) {
        return false;
      }

      if (!"categories".equals(ruri.get(2))) {
        return false;
      }

      if (!"widget-json-cats".equals(ruri.get(3))) {
        return false;
      }

      return "catsObj.json".equals(ruri.get(4));
    }
  }

  private static class WebcachePattern5 extends WebcacheMatcher {
    boolean match(final String urlStr,
                  final List<String> ruri) {
      if (ruri.size() != 9) {
        return false;
      }

      if (!"webcache".equals(ruri.get(0))) {
        return false;
      }

      if (!"v1.0".equals(ruri.get(1))) {
        return false;
      }

      if (!"jsonDays".equals(ruri.get(2))) {
        return false;
      }

      if (!isInt(ruri.get(3))) {
        return false;
      }

      if (!"list-json".equals(ruri.get(4))) {
        return false;
      }

      final String fexpr = ruri.get(5);

      if (!"no--filter".equals(fexpr) &&
              !onlyCatUids(fexpr)) {
        return false;
      }

      if (!isInt(ruri.get(6))) {
        return false;
      }

      if (!isInt(ruri.get(7))) {
        return false;
      }

      return isInt(ruri.get(8));
    }
  }

  private final static WebcacheMatcher[] webcacheMatchers = {
          new WebcachePattern1(),
          new WebcachePattern2(),
          new WebcachePattern3(),
          new WebcachePattern4(),
  };

  static abstract class WebcacheMatcher {
    int matched;

    abstract boolean match(String urlStr,
                           List<String> ruri);

    boolean isInt(final String s) {
      try {
        Integer.valueOf(s);
        return true;
      } catch (final Throwable ignored) {
        return false;
      }
    }
  }

  static boolean onlyCatUids(final String fexpr) {
      /*
    fexpr=(catuid='2962ac9d-4b307640-014b-32408a42-000054fa')&
    (catuid!='2962ac9d-2a425309-012a-43b52f6f-00000304'&catuid!='2962aca4-289343b8-0128-9420e1a5-00000007'&catuid!='2962aca4-289343b8-0128-9420505d-00000006')
   */
    final String frep = fexpr.replace("(", "").
            replace(")", "").
                                     replace("!=", "=");

    final String[] segs = frep.split("&");

    for (final String seg : segs) {
      if (!seg.startsWith("catuid=")) {
        return false;
      }
    }

    return true;
  }

  private void out(final String format, final Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out() {
    System.out.println();
  }
}