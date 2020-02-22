/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.LogEntry;
import org.bedework.bwcli.logs.ReqInOutLogEntry;
import org.bedework.util.misc.Util;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
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
  boolean summariseTests;

  int waitcountCount;
  LogEntry lastReqline;
  LogEntry lastEntry;

  boolean dumpIndented;

  final String wildflyStart = "[org.jboss.as] (Controller Boot Thread) WFLYSRV0025";

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

          outFmt("Long request %s %s %d: %s - %s %s",
                 rs.ip, rs.taskId, millis, rs.dt, dt, rs.request);
        }

        int ct = longreqIpMap.computeIfAbsent(rs.ip, k -> 0);

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
                         final boolean showMissingTaskIds,
                         final boolean summariseTests) {
    this.showLong = showLong;
    this.showMissingTaskIds = showMissingTaskIds;
    this.summariseTests = summariseTests;

    try {
      final Path logPath = Paths.get(logPathName);

      final File logFile = logPath.toFile();

      final LineNumberReader lnr = new LineNumberReader(new FileReader(logFile));

      while (true) {
        final String s = lnr.readLine();

        if (s == null) {
          break;
        }

        if (dumpIndented) {
          // dump the rest of some formatted output.
          if (s.startsWith(" ")) {
            out(s);
            continue;
          }

          dumpIndented = false;
        }

        if (infoLine(s)) {
          doInfo(s);
          continue;
        }

        if (summariseTests && debugLine(s)) {
          doSummariseTests(s);
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

  private void doInfo(final String s) {
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
      if (summariseTests) {
        lastReqline = rs;
      }

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
      if (summariseTests && waitcountCount <= 1) {
        outSummary(rs);
      }

      final ReqInOutLogEntry mapRs = tasks.get(rs.taskId);

      if (mapRs == null) {
        if (showMissingTaskIds) {
          final String dt = s.substring(0, s.indexOf(" INFO"));

          outFmt("Missing taskid %s %s",
                 rs.taskId, dt);
        }

        return;
      }

      if (mapRs.context == null) {
        outFmt("No context for %s %s", mapRs.dt, mapRs.request);

        return;
      }

      if (!mapRs.sameTask(rs)) {
        outFmt("Not same task %s\n %s", mapRs.toString(), rs.toString());

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

  private void doSummariseTests(final String s) {
    // Display various lines from the log
    // 2020-01-14 15:46:04,709 DEBUG [org.bedework.caldav.server.CaldavBWServlet] (default task-1) entry: PROPFIND
    final LogEntry le = new LogEntry();

    if (le.parse(s, null, "DEBUG") == null) {
      out(s + " ******************** Unparseable");
      return;
    }

    if (s.contains(" entry: ")) {
      lastEntry = le;
      return;
    }

    /* TODO - still not right....
       We should check the task id between a request in and out.
       If it's different then we have some interleaved request
     */

    /* If it's a WAITCOUNT and there's been a WAITCOUNT with no
       other task output just bump the count.
     */

    final var testUserAgentLabel = "User-Agent = \"Cal-Tester: ";
    final var isUserAgent = s.contains("User-Agent = \"");
    var isCalTest = s.contains(testUserAgentLabel);
    var isWaitcount = isCalTest && s.contains("WAITCOUNT ");

    if (isWaitcount) {
      //
      if (waitcountCount > 0) {
        waitcountCount++;
        return;
      }

      lastEntry = null;
      waitcountCount = 1;
    } else if (isUserAgent && (waitcountCount > 0)) {
      out(">---------------------------- WAITCOUNT = " + waitcountCount);
      waitcountCount = 0;
    }

    if (waitcountCount > 1) {
      return;
    }

    if (s.contains(" User-Agent = \"")) {
      outSummary(lastReqline);
      outSummary(lastEntry);
      var pos = le.logText.indexOf(testUserAgentLabel);
      if (pos >= 0) {
        le.logText = "------------- Test ---> " +
                le.logText.substring(0, pos) +
                le.logText.substring(pos + testUserAgentLabel.length(),
                                     le.logText.length() - 1) +
                "<------------------";
        outSummary(le);
      }

      return;
    }

    if (s.contains(" getRequestURI =")) {
      outSummary(le);
      return;
    }

    if (s.contains(" getRemoteUser =")) {
      outSummary(le);
      return;
    }

    if (s.contains("=BwInoutSched")) {
      outSchedSummary(le);
      return;
    }

  }

  private void outSummary(final LogEntry le) {
    if (le == null) {
      return;
    }
    outFmt("%s %-4s %-8s %s %s", le.dt,
           le.sinceLastMillis, le.sinceStartMillis,
           taskIdSummary(le), le.logText);
  }

  private String taskIdSummary(final LogEntry le) {
    if (le.taskId.startsWith("default ")) {
      return le.taskId.substring(8);
    }

    if (le.taskId.startsWith("org.bedework.bwengine:service=")) {
      return le.taskId.substring(30);
    }

    return le.taskId;
  }

  private void outSchedSummary(final LogEntry le) {
    var s = le.logText;

    if (s.contains("set event to")) {
      outSummary(le);
      return;
    }

    if (s.contains("Indexing to")) {
      outSummary(le);
    }

    if (s.contains("Add event with name")) {
      outSummary(le);
    }

    if (s.contains("Received messageEntityQueuedEvent")) {
      outSummary(le);
      dumpIndented = true;
    }
  }

  private ReqInOutLogEntry tryRequestLine(final String ln) {
    return tryRequestInOutLine(ln, "REQUEST");
  }

  private ReqInOutLogEntry tryRequestOut(final String ln) {
    return tryRequestInOutLine(ln, "REQUEST-OUT");
  }

  private ReqInOutLogEntry tryRequestInOutLine(final String ln,
                                               final String reqName) {
    if (!ln.contains(" " + reqName + ":")) {
      return null;
    }

    final ReqInOutLogEntry rs = new ReqInOutLogEntry();

    if (rs.parse(ln, reqName) < 0) {
      return null;
    }

    return rs;
  }

  private boolean infoLine(final String ln) {
    return ln.indexOf(" INFO ") == 23;
  }

  private boolean debugLine(final String ln) {
    return ln.indexOf(" DEBUG ") == 23;
  }

  private boolean tryErrorLine(final String ln) {
    if (ln.indexOf(" ERROR ") != 23) {
      return false;
    }

    errorLines++;

    return true;
  }

  private void results() {
    outFmt("Total requests: %d", totalRequests);
    if (totalForwardedRequests != totalRequests) {
      outFmt("Total forwarded requests: %d", totalForwardedRequests);
    }

    outFmt("Millis per request by context per 100 millis");

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

    outFmt("%s", header);

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

      outFmt("%s", l);
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

    outFmt("%s", sessReq);
    outFmt("%s", totReq);
    outFmt("%s", avgMs);
    outFmt("%s", "Figures ignoring highest bucket:");
    outFmt("%s", subTtotReq);
    outFmt("%s", subTavgMs);
    out();

    outFmt("Total error lines: %d", errorLines);

    out();

    final int numIps = 20;
    outFmt("List of top %d ips", numIps);

    final List<Map.Entry<String, Integer>> sorted =
            Util.sortMap(ReqInOutLogEntry.ipMap);
    int ct = 0;
    for (Map.Entry<String, Integer> ent: sorted) {
      outFmt("%s\t%d", ent.getKey(), ent.getValue());
      ct++;

      if (ct > numIps) {
        break;
      }
    }

    out();

    outFmt("List of top %d long request ips", numIps);

    final List<Map.Entry<String, Integer>> longSorted =
            Util.sortMap(longreqIpMap);
    ct = 0;
    for (Map.Entry<String, Integer> ent: longSorted) {
      outFmt("%s\t%d", ent.getKey(), ent.getValue());
      ct++;

      if (ct > numIps) {
        break;
      }
    }
  }

  private void outFmt(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out(final String val) {
    System.out.println(val);
  }

  private void error(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out() {
    System.out.println();
  }
}
