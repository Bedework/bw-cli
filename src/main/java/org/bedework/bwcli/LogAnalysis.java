/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.util.misc.ToString;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Q&D log analyzer. Here so we can run it from the cli
 *
 * <p>A log line looks like (this is one line)<pre>
 * 2019-01-04 00:00:11,742 INFO  [org.bedework.webcommon.search.RenderSearchResultAction] (default task-27) REQUEST:rFtrI_S0o_P8sp0fa9cm7ZvR9a5aK6NkZ1Ml8oSF:unknown:charset=UTF-8:10.0.250.197:http://calendar.yale.edu/cal/main/showMainEventList.rdo - Referer:http://calendar.yale.edu/cal/main/showEventList.rdo;jsessionid=rFtrI_S0o_P8sp0fa9cm7ZvR9a5aK6NkZ1Ml8oSF.ip-10-0-10-5 - X-Forwarded-For:117.222.245.27
 * </pre>
 *
 * <p>or this one
 *
 * <pre>2019-03-15 15:20:22,912 INFO  [org.bedework.webcommon.BwCallbackImpl] (default task-4) REQUEST-OUT:MYISJK5RJkg3NkoW6XKCJpfG_R6v106z83Xg9Nnz:bwclientcb:charset=UTF-8:10.0.250.197:http://calendar.yale.edu/cal/event/eventView.do;jsessionid=yb4n2K2XFwM1yJV0RCt0k2FHUx2EQP0uEVAt7Nlk.ip-10-0-10-189?b=de&href=%2Fpublic%2Fcals%2FMainCal%2FCAL-ff808081-6831cab0-0168-33304e60-00003754.ics - Referer:NONE - X-Forwarded-For:54.70.40.11</pre>
 *
 * User: mike Date: 1/4/19 Time: 22:43
 */
public class LogAnalysis {
  long totalRequests;
  long totalForwardedRequests;
  long errorLines;
  long unterminatedTask;
  boolean showLong;
  boolean showMissingTaskIds;

  final String wildflyStart = "[org.jboss.as] (Controller Boot Thread) WFLYSRV0025";

  // base for REQUEST and REQUEST-OUT
  class LogEntry {
    String req;
    int curPos; // while parsing

    Long millis;
    String dt;
    String taskId;

    String logName;

    String sessionId;

    String logPrefix;

    String charset;

    /**
     * @param req log entry
     * @return position we reached or null for bad record
     */
    Integer parse(final String req,
                  final String logName) {
      this.req = req;
      this.logName = logName;
      dt = req.substring(0, req.indexOf(" INFO"));
      millis = millis();
      if (millis == null) {
        error("Unable to get millis for %s", req);
        return null;
      }

      taskId = taskId(req);

      curPos = req.indexOf(logName + ":");

      if (curPos < 0) {
        error("No name found for %s", req);
        return null;
      }

      if (!logName.equals(field())) {
        error("Expected %s for %s", logName, req);
        return null;
      }

      sessionId = field();
      if (sessionId == null) {
        error("No session end found for %s", req);
        return null;
      }

      logPrefix = field();

      charset = field();
      if (charset == null) {
        error("No charset found for %s", req);
        return null;
      }

      return curPos;
    }

    boolean sameTask(final LogEntry otherEntry) {
      if (!taskId.equals(otherEntry.taskId)) {
        out("taskId mismatch");
        return false;
      }

      if (!sessionId.equals(otherEntry.sessionId)) {
        out("sessionId mismatch");
        return false;
      }

      if (!logPrefix.equals(otherEntry.logPrefix)) {
        out("logPrefix mismatch");
        return false;
      }

      if (!charset.equals(otherEntry.charset)) {
        out("charset mismatch");
        return false;
      }

      return true;
    }

    public Long millis() {
      try {
        // 2019-01-04 00:00:11,742 ...
        // 0123456789012345678901234

        final long hrs = Integer.valueOf(req.substring(11, 13));
        final long mins = Integer.valueOf(req.substring(14, 16));
        final long secs = Integer.valueOf(req.substring(17, 19));
        final long millis = Integer.valueOf(req.substring(20, 23));

        return ((((hrs * 60) + mins) * 60) + secs) * 1000 + millis;
      } catch (final Throwable ignored) {
        return null;
      }
    }

    // Expect this next
    String field() {
      return field("");
    }

    // Needed because ipv6 addresses have ':'
    String field(final String nextFieldStart) {
      final int start = curPos;
      final int end = req.indexOf(":" + nextFieldStart, start);
      if (end < 0) {
        error("No end found for %s", req);
        return null;
      }

      final String res = req.substring(start, end);
      curPos = end + 1; // Skip only the ":"

      return res;
    }

    void toStringSegment(final ToString ts) {
      ts.append("taskId", taskId);
      ts.append("sessionId", sessionId);
      ts.append("logPrefix", logPrefix);
      ts.append("charset", charset);
    }

    public String toString() {
      final ToString ts = new ToString(this);

      toStringSegment(ts);

      return ts.toString();
    }
  }

  private String taskId(final String ln) {
    final int taskIdPos = ln.indexOf("] (default");
    if (taskIdPos < 0) {
      return null;
    }

    final int endTaskIdPos = ln.indexOf(")", taskIdPos);

    if (endTaskIdPos < 0) {
      return null;
    }

    return ln.substring(taskIdPos, endTaskIdPos + 1);
  }

  class ReqInOutLogEntry extends LogEntry {
    String ip;

    String url;

    boolean unparseable;

    List<NameValuePair> params;

    String context;
    String request;

    String referer;
    String xForwardedFor;

    Integer parse(final String req,
                  final String logName) {
      if (super.parse(req, logName) == null) {
        return null;
      }

      ip = field("http");
      if (ip == null) {
        error("No ip for %s", req);
        return null;
      }

      url = dashField();
      if (url == null) {
        error("No url for %s", req);
        return null;
      }

      String fname = field();
      if (!"Referer".equals(fname)) {
        error("Expected referer for %s", req);
        return null;
      }

      referer = dashField();

      fname = field();
      if (!"X-Forwarded-For".equals(fname)) {
        error("Expected X-Forwarded-For for %s", req);
        return null;
      }

      // I think it's always the last field
      xForwardedFor = req.substring(curPos);

      if (!xForwardedFor.equals("NONE")) {
        ip = xForwardedFor;
      }

      Integer ct = ipMap.computeIfAbsent(ip, k -> 0);

      ct = ct + 1;
      ipMap.put(ip, ct);

      // Parse out the url
      int urlPos = 10; // safely past the "//"

      final int reqPos = urlPos;

      urlPos = url.indexOf("/", urlPos);
      if (urlPos < 0) {
        // No context
        return curPos;
      }

      urlPos++; // past the /

      final int endContextPos = url.indexOf("/", urlPos);
      if (endContextPos < 0) {
        return curPos;
      }

      try {
        context = url.substring(urlPos, endContextPos);
        request = url.substring(reqPos);

        if (context.trim().length() == 0) {
          context = null;
        }
      } catch (final Throwable t) {
        out("%s", req);
        out("%s: %s: %s ",
            urlPos, endContextPos,
            reqPos);
        return null;
      }

      try {
        params = URLEncodedUtils.parse(new URI(url),
                                       Charset.forName("UTF-8"));
        unparseable = false;
      } catch (final Throwable ignored) {
        unparseable = true;
      }

      return curPos;
    }

    boolean sameTask(final ReqInOutLogEntry otherEntry) {
      if (!super.sameTask(otherEntry)) {
        return false;
      }

      if (!ip.equals(otherEntry.ip)) {
        out("ip mismatch");
        return false;
      }

      if (!url.equals(otherEntry.url)) {
        out("url mismatch");
        return false;
      }

      return true;
    }

    boolean hasJsessionid() {
      return (url != null) && url.contains(";jsessionid=");
    }

    String dashField() {
      final int start = curPos;
      final int end = req.indexOf(" - ", start);
      if (end < 0) {
        error("No request found for %s", req);
        return null;
      }

      final String res = req.substring(start, end);
      curPos = end + 3;

      return res;
    }

    void toStringSegment(final ToString ts) {
      super.toStringSegment(ts);

      ts.append("ip", ip);
      ts.append("url", url);
    }
  }

  final Map<String, Integer> ipMap = new HashMap<>();
  final Map<String, Integer> longreqIpMap = new HashMap<>();
  final Map<String, ReqInOutLogEntry> tasks = new HashMap<>();

  final static int numMilliBuckets = 20;
  final static int milliBucketSize = 100;

  class ContextInfo {
    String context;
    long requests;
    long totalMillis;

    // Total ignoring the highest bucket
    long subTrequests;
    long subTtotalMillis;

    long[] buckets = new long[numMilliBuckets];
    long rTotalReq; // Used for output

    // How often we see ";jsessionid" in the incoming request
    long sessions;

    ContextInfo(final String context) {
      this.context = context;
    }

    void reqOut(final String ln,
                final ReqInOutLogEntry rs,
                final long millis) {
      requests++;
      totalMillis += millis;

      int bucket = (int)(millis / milliBucketSize);

      if (bucket >= (numMilliBuckets - 1)) {
        bucket = numMilliBuckets - 1;
        if (showLong) {
          final String dt = ln.substring(0, ln.indexOf(" INFO"));

          out("Long request %s %s %d: %s - %s %s",
              rs.ip, rs.taskId, millis, rs.dt, dt, rs.request);
        }

        Integer ct = longreqIpMap.computeIfAbsent(rs.ip, k -> 0);

        ct = ct + 1;
        longreqIpMap.put(rs.ip, ct);
      }

      buckets[bucket]++;

      if (bucket < (numMilliBuckets - 1)) {
        subTrequests++;
        subTtotalMillis += millis;
      }
    }
  }

  final Map<String, ContextInfo> contexts = new HashMap<>();

  public boolean process(final String logPathName,
                         final boolean showLong,
                         final boolean showMissingTaskIds) {
    this.showLong = showLong;
    this.showMissingTaskIds = showMissingTaskIds;

    try {
      final Path logPath = Paths.get(logPathName);

      final File logFile = logPath.toFile();

      final LineNumberReader lnr = new LineNumberReader(new FileReader(logFile));

      while (true) {
        final String s = lnr.readLine();

        if (s == null) {
          break;
        }

        if (infoLine(s)) {
          doInfo(s);
          continue;
        }

        tryErrorLine(s);
      }

      results();

      return true;
    } catch (final Throwable t) {
      t.printStackTrace();
      return false;
    }
  }

  private void doInfo(final String s) throws Throwable {
    if (s.contains(wildflyStart)) {
      // Wildfly restarted
      tasks.clear();
      return;
    }

    //if (s.startsWith("2019-01-08 09:00:44,209")) {
    //  out("the line");
    //}

    ReqInOutLogEntry rs = tryRequestLine(s);

    if (rs != null) {
      final ReqInOutLogEntry mapRs = tasks.get(rs.taskId);

      if (mapRs != null) {
        // No request-out message
        unterminatedTask++;
      }

      tasks.put(rs.taskId, rs);

      return;
    }

    rs = tryRequestOut(s);

    if (rs != null) {
      final ReqInOutLogEntry mapRs = tasks.get(rs.taskId);

      if (mapRs == null) {
        if (showMissingTaskIds) {
          final String dt = s.substring(0, s.indexOf(" INFO"));

          out("Missing taskid %s %s",
              rs.taskId, dt);
        }

        return;
      }

      if (mapRs.context == null) {
        out("No context for %s %s", mapRs.dt, mapRs.request);

        return;
      }

      if (!mapRs.sameTask(rs)) {
        out("Not same task %s\n %s", mapRs.toString(), rs.toString());

        return;
      }

      final long reqMillis = rs.millis - mapRs.millis;

      ContextInfo ci =
              contexts.computeIfAbsent(mapRs.context,
                                       k -> new ContextInfo(mapRs.context));

      ci.reqOut(s, mapRs, reqMillis);

      if (rs.hasJsessionid()) {
        ci.sessions++;
      }

      // Done with the entry
      tasks.remove(rs.taskId);
    }
  }

  private ReqInOutLogEntry tryRequestLine(final String ln) throws Throwable {
    return tryRequestInOutLine(ln, "REQUEST");
  }

  private ReqInOutLogEntry tryRequestOut(final String ln) throws Throwable {
    return tryRequestInOutLine(ln, "REQUEST-OUT");
  }

  private ReqInOutLogEntry tryRequestInOutLine(final String ln,
                                               final String reqName) throws Throwable {
    if (!ln.contains(" " + reqName + ":")) {
      return null;
    }

    final ReqInOutLogEntry rs = new ReqInOutLogEntry();

    if (rs.parse(ln, reqName) < 0) {
      return null;
    }

    return rs;
  }

  private boolean infoLine(final String ln) throws Throwable {
    return ln.indexOf(" INFO ") == 23;
  }

  private boolean tryErrorLine(final String ln) throws Throwable {
    if (ln.indexOf(" ERROR ") != 23) {
      return false;
    }

    errorLines++;

    return true;
  }

  private void results() {
    out("Total requests: %d", totalRequests);
    if (totalForwardedRequests != totalRequests) {
      out("Total forwarded requests: %d", totalForwardedRequests);
    }

    out("Millis per request by context per 100 millis");

    final Set<String> contextNames = new TreeSet<>(contexts.keySet());

    final String labelPattern = " %6s |";

    final ContextInfo[] cis = new ContextInfo[contextNames.size()];
    final String[] cellFormats = new String[contextNames.size()];
    final String[] hdrFormats = new String[contextNames.size()];
    int cx = 0;

    final StringBuilder header =
            new StringBuilder(String.format(labelPattern, ""));

    for (final String context: contextNames) {
      final ContextInfo ci = contexts.get(context);
      cis[cx] = ci;

      final int fldLen = Math.max(context.length(), 5);
      cellFormats[cx] = " %" + fldLen + "s - %3s%% |";
      final String hdrFmt  = "        %" + fldLen + "s |";
      hdrFormats[cx] = hdrFmt;
      header.append(String.format(hdrFmt, context));
      cx++;
    }

    out("%s", header);

    // Output each bucket for each context

    for (int i = 0; i < numMilliBuckets; i++) {
      final StringBuilder l =
              new StringBuilder(String.format(labelPattern, "<" + ((i + 1) * 100)));

      for (int j = 0; j < cis.length; j++) {
        final ContextInfo ci = cis[j];
        // bucket and percent

        l.append(String.format(cellFormats[j],
                 ci.buckets[i],
                 ((int)(100 * ci.rTotalReq / ci.requests))));

        ci.rTotalReq += ci.buckets[i];
      }

      out("%s", l);
    }

    // Total lines

    final StringBuilder sessReq =
            new StringBuilder(String.format(labelPattern, "Sess"));
    final StringBuilder totReq =
            new StringBuilder(String.format(labelPattern, "Total"));
    final StringBuilder avgMs =
            new StringBuilder(String.format(labelPattern, "Avg ms"));

    final StringBuilder subTtotReq =
            new StringBuilder(String.format(labelPattern, "Total"));
    final StringBuilder subTavgMs =
            new StringBuilder(String.format(labelPattern, "Avg ms"));

    for (int j = 0; j < cis.length; j++) {
      final ContextInfo ci = cis[j];

      sessReq.append(String.format(hdrFormats[j],
                                  ci.sessions));
      totReq.append(String.format(hdrFormats[j],
                                  ci.requests));
      avgMs.append(String.format(hdrFormats[j],
                                 (int)(ci.totalMillis / ci.requests)));

      subTtotReq.append(String.format(hdrFormats[j],
                                      ci.subTrequests));
      subTavgMs.append(String.format(hdrFormats[j],
                                     (int)(ci.subTtotalMillis / ci.subTrequests)));
    }

    out("%s", sessReq);
    out("%s", totReq);
    out("%s", avgMs);
    out("%s", "Figures ignoring highest bucket:");
    out("%s", subTtotReq);
    out("%s", subTavgMs);
    out();

    out("Total error lines: %d", errorLines);

    out();

    final int numIps = 20;
    out("List of top %d ips", numIps);

    final List<Map.Entry<String, Integer>> sorted =
            sortMap(ipMap);
    int ct = 0;
    for (Map.Entry<String, Integer> ent: sorted) {
      out("%s\t%d", ent.getKey(), ent.getValue());
      ct++;

      if (ct > numIps) {
        break;
      }
    }

    out();

    out("List of top %d long request ips", numIps);

    final List<Map.Entry<String, Integer>> longSorted =
            sortMap(longreqIpMap);
    ct = 0;
    for (Map.Entry<String, Integer> ent: longSorted) {
      out("%s\t%d", ent.getKey(), ent.getValue());
      ct++;

      if (ct > numIps) {
        break;
      }
    }
  }

  private <K, V extends Comparable> List<Map.Entry<K, V>> sortMap(Map<K, V> map) {
    // We create a list from the elements of the unsorted map
    List <Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());

    // Now sort the list
    Comparator<Map.Entry<K, V>> comparator =
            Comparator.comparing(Map.Entry<K, V>::getValue);
    list.sort(comparator.reversed());

    return list;
  }

  private void out(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void error(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out() {
    System.out.println();
  }
}
