/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.LogEntry;
import org.bedework.bwcli.logs.ReqInOutLogEntry;

import java.util.Arrays;
import java.util.List;

/**
 * User: mike Date: 10/23/22 Time: 21:53
 */
public class DisplaySessions extends LogAnalysis {
  private final String taskId;
  private final String user;
  private final boolean skipAnon;

  private ReqInOutLogEntry lastMapRs;

  private final List<String> skipClasses =
          Arrays.asList("org.apache.struts2",
                        "org.bedework.timezones.server.",
                        "org.bedework.util.servlet.HttpServletUtils");

  private final List<String> skipContent =
          Arrays.asList(
                  "No form in session",
                  "==============",
                  "actionType:",
                  "conversation: ",
                  "Request parameters",
                  "entry",
                  "query=null",
                  "contentlen=",
                  "parameters:",
                  "Request parameters",
                  "Set presentation state",
                  "java.sql.Connection#beginRequest has been invoked",
                  "Obtained state",
                  "Setting locale to ",
                  "About to prepare render",
                  "XSLTFilter: Converting",
                  "getWriter called",
                  "out Obtained BwCallback object",
                  "Request out for module default",
                  "About to flush",
                  "java.sql.Connection#endRequest has been invoked");

  public DisplaySessions(final String taskId,
                         final String user,
                         final boolean skipAnon) {
    this.taskId = taskId;
    this.user = user;
    this.skipAnon = skipAnon;
  }

  public void processRecord(final String s) {
    // Display various lines from the log
    // 2020-01-14 15:46:04,709 DEBUG [org.bedework.caldav.server.CaldavBWServlet] (default task-1) entry: PROPFIND

    final LogEntry le = new LogEntry();

    if(!s.startsWith("202")) {
      // Continuation line - we'll add to last request we saw -
      // This may not be correct if requests overlap
      le.unparsed(s);
      if (lastMapRs != null) {
        if (lastMapRs.doingCalsuite) {
          final var nmstr = "  name=";
          final var spos = s.indexOf(nmstr);

          if (spos >= 0) {
            final var pos = s.indexOf(",", spos);
            if (pos < 0) {
              le.unparsed(s);
            } else {
              lastMapRs.calsuiteName = s.substring(spos + nmstr.length(),
                                                   pos);
            }
          }
        } else {
          lastMapRs.addLogEntry(le);
        }
      }

      return;
    }

    if (le.parse(s, null, null) == null) {
      le.unparsed(s);
      if (lastMapRs != null) {
        lastMapRs.addLogEntry(le);
      }
      return;
    }

    if ("ChangeNotifications".equals(le.taskId)) {
      return;
    }

    if (le.className != null) {
      for (final var c: skipClasses) {
        if (le.className.startsWith(c)) {
          return;
        }
      }
    }

    if (le.logText != null) {
      for (final var c: skipContent) {
        if (le.logText.startsWith(c)) {
          return;
        }
      }
    }

    if ((taskId != null) && !taskId.equals(le.taskId)) {
      return;
    }

    final ReqInOutLogEntry mapRs = tasks.get(le.taskId);

    if (mapRs == null) {
      // No associated request
      outFmt("No task %s found for %s", le.taskId, s);
      return;
    }

    lastMapRs = mapRs;

    if (mapRs.skipping) {
      return;
    }

    // getRemoteUser = bnjones-admin

    final String lt = le.logText;
    final String gru = "getRemoteUser = ";
    final String gruri = "getRequestURI = ";
    final String grsess = "getRequestedSessionId = ";
    final String fcs = "Found calSuite BwCalSuiteWrapper";

    if (lt.startsWith(gru)) {
      final var loguser = lt.substring(gru.length());

      if (skipAnon && "null".equals(loguser)) {
        mapRs.skipping = true;
        return;
      }

      if ((user != null) && !user.equals(loguser)) {
        mapRs.skipping = true;
        return;
      }

      mapRs.user = loguser;
    } else if (lt.startsWith(gruri)) {
      mapRs.uri = lt.substring(gruri.length());
    } else if (lt.startsWith(grsess)) {
      mapRs.sessid = lt.substring(grsess.length());
    } else if (lt.startsWith(fcs)) {
      mapRs.doingCalsuite = true;
    } else {
      mapRs.addLogEntry(le);
    }
  }

  @Override
  public void requestOut(final ReqInOutLogEntry rsin,
                         final ReqInOutLogEntry rsout) {
    if (rsin.skipping) {
      return;
    }

    if ((taskId != null) && !taskId.equals(rsin.taskId)) {
      return;
    }

    if ((user != null) && !user.equals(rsin.user)) {
      return;
    }

    // Output the log entries

    outFmt("Request in: %s out %s task %s", rsin.dt, rsout.dt, rsin.taskId);
    outFmt("     uri: %s", rsin.uri);
    outFmt("    user: %s", rsin.user);
    if (rsin.calsuiteName != null) {
      outFmt("calsuite: %s", rsin.calsuiteName);
    } else {
      outFmt("calsuite: %s", "NONE");
    }

    for (final var le: rsin.entries) {
      outFmt("         %s", le.logText);
    }
  }
}
