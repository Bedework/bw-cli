/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.logs;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
 * User: mike Date: 10/23/22 Time: 00:31
 */
public abstract class LogReader {
  protected boolean dumpIndented;
  protected LogEntry lastReqline;
  protected long errorLines;
  boolean showLong;
  boolean showMissingTaskIds;

  protected long unterminatedTask;

  protected final Map<String, ReqInOutLogEntry> tasks = new HashMap<>();

  protected final Map<String, ContextInfo> contexts = new HashMap<>();

  protected static Map<String, Integer> longreqIpMap = new HashMap<>();

  final String wildflyStart = "[org.jboss.as] (Controller Boot Thread) WFLYSRV0025";

  public abstract void processRecord(final String s);

  public abstract void processInfo(final ReqInOutLogEntry rs);

  public void requestOut(final ReqInOutLogEntry rsin,
                         final ReqInOutLogEntry rsout) {
  }

  public abstract void results();

  public void process(final String logPathName,
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

        processRecord(s);

        checkErrorLine(s);
      }

      results();
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  private void doInfo(final String s) {
    if (wildflyStart(s)) {
      // Wildfly restarted
      tasks.clear();
      return;
    }

    ReqInOutLogEntry rs = tryRequestLine(s);

    if (rs != null) {
      lastReqline = rs;

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
      processInfo(rs);

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
      final ContextInfo ci =
              contexts.computeIfAbsent(mapRs.context,
                                       k -> new ContextInfo(mapRs.context,
                                                            longreqIpMap));

      ci.reqOut(s, mapRs, reqMillis, showLong);

      if (rs.hasJsessionid()) {
        ci.sessions++;
      }

      requestOut(mapRs, rs);

      // Done with the entry
      tasks.remove(rs.taskId);
    }
  }

  protected boolean wildflyStart(final String ln) {
    return ln.contains(wildflyStart);
  }

  protected boolean infoLine(final String ln) {
    return ln.indexOf(" INFO ") == 23;
  }

  protected boolean debugLine(final String ln) {
    return ln.indexOf(" DEBUG ") == 23;
  }

  protected void checkErrorLine(final String ln) {
    if (ln.indexOf(" ERROR ") != 23) {
      return;
    }

    errorLines++;
  }

  private ReqInOutLogEntry tryRequestLine(final String ln) {
    return tryRequestInOutLine(ln, true);
  }

  private ReqInOutLogEntry tryRequestOut(final String ln) {
    return tryRequestInOutLine(ln, false);
  }

  private ReqInOutLogEntry tryRequestInOutLine(final String ln,
                                               final boolean in) {
    final ReqInOutLogEntry rs = new ReqInOutLogEntry();
    final Integer res = rs.parse(ln, in);

    if ((res == null) || (res < 0)) {
      return null;
    }

    return rs;
  }

  public void outSummary(final LogEntry le) {
    if (le == null) {
      return;
    }
    outFmt("%s %-4s %-8s %s %s", le.dt,
           le.sinceLastMillis, le.sinceStartMillis,
           taskIdSummary(le), le.logText);
  }

  public String taskIdSummary(final LogEntry le) {
    if (le.taskId.startsWith("default ")) {
      return le.taskId.substring(8);
    }

    if (le.taskId.startsWith("org.bedework.bwengine:service=")) {
      return le.taskId.substring(30);
    }

    return le.taskId;
  }

  protected void outFmt(final String format,
                      final Object... args) {
    System.out.println(String.format(format, args));
  }

  protected void out(final String val) {
    System.out.println(val);
  }

  protected void out() {
    System.out.println();
  }
}
