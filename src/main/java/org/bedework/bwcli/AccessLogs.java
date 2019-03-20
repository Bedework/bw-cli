/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Used to check access logs for useful info.
 *
 * User: mike Date: 3/12/19 Time: 13:40
 */
public class AccessLogs {
  int numLegacy;

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
  int numPattern1;

  public boolean legacyFeeds(final String logPathName) {
    try {
      final Path logPath = Paths.get(logPathName);

      final File logFile = logPath.toFile();

      final LineNumberReader lnr = new LineNumberReader(
              new FileReader(logFile));

      while (true) {
        final String s = lnr.readLine();

        if (s == null) {
          break;
        }

        if (legacyFeeder(s)) {
          doLegacyFeeder(s);
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

  private void results() {
    out("Total legacy requests: %d", numLegacy);
    out("Total pattern1 requests: %d", numPattern1);
  }

  private boolean legacyFeeder(final String s) throws Throwable {
    return s.contains("GET /feeder/")
            && (s.contains("?calPath=") || s.contains("&calPath="));
  }

  private void doLegacyFeeder(final String s) throws Throwable {
    numLegacy++;

    final int start = s.indexOf("GET /feeder/") + 4;
    final int end = s.indexOf(" HTTP/1.1");

    final String urlStr = s.substring(start, end);

    final URI uri = new URI(urlStr);

    List<NameValuePair> params = URLEncodedUtils
            .parse(uri, Charset.forName("UTF-8"));

    final Map<String, List<String>> paramsMap = new HashMap<>();

    for (NameValuePair param : params) {
      paramsMap.computeIfAbsent(param.getName(),
                                k -> new ArrayList<>())
               .add(param.getValue());
    }

    if (pattern1(urlStr, params, paramsMap)) {
      numPattern1++;
      return;
    }
  }

  private boolean pattern1(final String urlStr,
                           final List<NameValuePair> params,
                           final Map<String, List<String>> paramsMap) {
    if (!urlStr.startsWith("/feeder/main/listEvents.do")) {
      return false;
    }

    if (params.size() != 6) {
      return false;
    }


    if (paramsMap.size() != 5) {
      return false;
    }

    if (!checkParam(paramsMap, "calPath", 1) &&
            !checkParam(paramsMap, "skinName", 1) &&
            !checkParam(paramsMap, "setappvar", 2) &&
            !checkParam(paramsMap, "fexpr", 1) &&
            !checkParam(paramsMap, "days", 1)) {
      return false;
    }

    if (!"list-json".equals(paramsMap.get("skinName").get(0))) {
      return false;
    }

    return onlyCatUid(paramsMap.get("fexpr").get(0));
  }

  private boolean checkParam(final Map<String, List<String>> paramsMap,
                             final String name,
                             final int num) {
    final List<String> vals = paramsMap.get(name);

     return (vals != null) && (vals.size() == num);
  }

  private boolean onlyCatUid(final String fexpr) {
    /*
      fexpr=(catuid='2962aca4-289343b8-0128-98411c36-0000001e')
            012345678901234567890123456789012345678901234567890
     */


    return fexpr.startsWith("(catuid='") && (fexpr.length() == 51) &&
            fexpr.endsWith("')");
  }

  private void out(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out() {
    System.out.println();
  }
}