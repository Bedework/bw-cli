/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.AccessLogEntry;
import org.bedework.util.misc.Util;

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
import java.util.Collections;
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

  public static class AccessPeriod {
    final Map<String, Integer> ipCounts = new HashMap<>();
    final Map<String, Integer> ip2Counts = new HashMap<>();
    final int periodSeconds;

    AccessPeriod(final int periodSeconds) {
      this.periodSeconds = periodSeconds;
    }

    void addIp(final String ip) {
      var i = ipCounts.getOrDefault(ip, 0);
      ipCounts.put(ip, i + 1);

      var ip2 = getIp2(ip);
      if (ip2 == null) {
        return;
      }

      i = ip2Counts.getOrDefault(ip2, 0);
      ip2Counts.put(ip2, i + 1);
    }

    int totalRequests() {
      return ipCounts.values().stream().mapToInt(Number::intValue).sum();
    }

    float perSecond() {
      return (float)totalRequests() / periodSeconds;
    }

    void add(final AccessPeriod ap) {
      for (var ip: ap.ipCounts.keySet()) {
        var ct = ap.ipCounts.get(ip);

        var i = ipCounts.getOrDefault(ip, 0);
        ipCounts.put(ip, i + ct);
      }

      for (var ip2: ap.ip2Counts.keySet()) {
        var ct = ap.ip2Counts.get(ip2);

        var i = ip2Counts.getOrDefault(ip2, 0);
        ipCounts.put(ip2, i + ct);
      }
    }

    String getIp2(final String ip) {
      var pos = ip.indexOf(".");
      if (pos < 0) {
        return null;
      }

      pos = ip.indexOf(".", pos + 1);
      if (pos < 0) {
        return null;
      }

      return ip.substring(0, pos) + ".*";
    }
  }

  final static int hourSecs = 60 * 60;

  public static class AccessDay extends AccessPeriod {
    final AccessPeriod[] hours = new AccessPeriod[24];

    AccessDay() {
      super(hourSecs * 24);

      for (int i = 0; i <= 23; i++) {
        hours[i] = new AccessPeriod(hourSecs);
      }
    }

    void addIp(final String ip,
               final int hour) {
      hours[hour].addIp(ip);
      addIp(ip);
    }
  }

  public static Map<String, AccessDay> dayValues = new HashMap<>();

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

        final AccessDay dayVal =
                dayValues.computeIfAbsent(ale.normDate, v -> new AccessDay());
        dayVal.addIp(ale.ip, ale.hourOfDay);

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
      var msg = "No such file: " + logPathName;
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

    final List<String> days = new ArrayList<>(dayValues.keySet());
    Collections.sort(days);

    for (final String day: days) {
      final AccessDay dayVal = dayValues.get(day);

      out();

      out1day(day, dayVal);
    }
  }

  private void out1day(final String day,
                       final AccessDay dayVal) {
    out("Ip counts for %s", day);
    out();

    final List<Map.Entry<String, Integer>> longSorted =
            Util.sortMap(dayVal.ipCounts);

    long total = 0L;

    for (Map.Entry<String, Integer> ent: longSorted) {
      int ct = ent.getValue();
      total += ct;
      outFmt("%-15s\t%d", ent.getKey(), ct);
    }

    out();
    out("Total: %s", total);

    out("Ip domain counts for %s", day);
    out();

    final List<Map.Entry<String, Integer>> long2Sorted =
            Util.sortMap(dayVal.ip2Counts);

    total = 0L;

    for (Map.Entry<String, Integer> ent: long2Sorted) {
      int ct = ent.getValue();
      total += ct;
      outFmt("%-15s\t%d", ent.getKey(), ct);
    }

    out();
    out("Total: %s", total);

    out("Avg requests per minute for each hour:");
    int i = 0;

    for (final AccessPeriod ap: dayVal.hours) {
      out("%2s: %.2f", i, ap.perSecond() * 60);
      i++;
    }

    out("Avg requests per minute for day: %.2f", dayVal.perSecond() * 60);
  }

  private void outFmt(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void doLegacyFeeder(final AccessLogEntry ale) {
    numLegacy++;

    final URI uri;
    try {
      uri = new URI(ale.path);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");

    final Map<String, List<String>> paramsMap = new HashMap<>();

    for (NameValuePair param : params) {
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
    } catch (URISyntaxException e) {
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
    } catch (Throwable t) {
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

    ArrayList<String> al = new ArrayList<>();
    while (st.hasMoreTokens()) {
      String s = st.nextToken();

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
  private class FeedPattern1 extends FeedMatcher {
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
  private class FeedPattern2 extends FeedMatcher {
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

      if (!checkParVal(paramsMap.get("skinName").get(0),
                       "list-rss", "list-json", "default")) {
        return false;
      }

      return true;
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
  private class FeedPattern3 extends FeedMatcher {
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
  private class FeedPattern4 extends FeedMatcher {
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

      if (!checkParVal(paramsMap.get("skinName").get(0),
                       "list-rss")) {
        return false;
      }

      return true;
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
  private class FeedPattern5 extends FeedMatcher {
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

      if (!checkParVal(paramsMap.get("skinName").get(0),
                       "list-json")) {
        return false;
      }

      return true;
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
  private class FeedPattern6 extends FeedMatcher {
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
  private class FeedPattern7 extends FeedMatcher {
    boolean match(final String urlStr,
                  final List<NameValuePair> params,
                  final Map<String, List<String>> paramsMap) {
      if (!check(urlStr, "/feeder/main/listEvents.do",
                 params, 4, paramsMap, 4)) {
        return false;
      }

      if (!checkParam(paramsMap, "calPath", 1) ||
              !checkParam(paramsMap, "format", 1) ||
              !checkParam(paramsMap, "setappvar", 1) ||
              !checkParam(paramsMap, "days", 1)) {
        return false;
      }

      return true;
    }
  }

  /* Pattern 8 is of this form:
    /feeder/widget/categories.do
       Request params:
        skinName=widget-json-cats
        setappvar=objName(catsObj)
        calPath=/public/cals/MainCal
   */
  private class FeedPattern8 extends FeedMatcher {
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
  private class FeedPattern9 extends FeedMatcher {
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
  private class FeedPattern10 extends FeedMatcher {
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

  private FeedMatcher[] feedMatchers = {
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

    abstract boolean match(final String urlStr,
                           final List<NameValuePair> params,
                           final Map<String, List<String>> paramsMap);

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

  private class WebcachePattern1 extends WebcacheMatcher {
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

      if (!"bwObject.json".equals(ruri.get(6)) &&
              !"no--object.json".equals(ruri.get(6))) {
        return false;
      }

      return true;
    }
  }

  private class WebcachePattern2 extends WebcacheMatcher {
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

      if (!"no--filter.rss".equals(fexpr) &&
              !"no--filter.xml".equals(fexpr) &&
              !onlyCatUids(fexpr)) {
        return false;
      }

      return true;
    }
  }

  private class WebcachePattern3 extends WebcacheMatcher {
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

      if (!"no--filter.ics".equals(fexpr) &&
              !onlyCatUids(fexpr)) {
        return false;
      }

      return true;
    }
  }

  private class WebcachePattern4 extends WebcacheMatcher {
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


      if (!"catsObj.json".equals(ruri.get(4))) {
        return false;
      }

      return true;
    }
  }

  private class WebcachePattern5 extends WebcacheMatcher {
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

      if (!isInt(ruri.get(8))) {
        return false;
      }

      return true;
    }
  }

  private WebcacheMatcher[] webcacheMatchers = {
          new WebcachePattern1(),
          new WebcachePattern2(),
          new WebcachePattern3(),
          new WebcachePattern4(),
  };

  abstract class WebcacheMatcher {
    int matched;

    abstract boolean match(final String urlStr,
                           final List<String> ruri);

    boolean isInt(final String s) {
      try {
        //noinspection ResultOfMethodCallIgnored
        Integer.valueOf(s);
        return true;
      } catch (final Throwable ignored) {
        return false;
      }
    }
  }

  boolean onlyCatUids(final String fexpr) {
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

  private void out(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out() {
    System.out.println();
  }
}