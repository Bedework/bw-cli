/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.LogEntry;
import org.bedework.bwcli.logs.ReqInOutLogEntry;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * User: mike Date: 10/23/22 Time: 21:53
 */
public class DisplaySessions extends LogAnalysis {
  private final String taskId;
  private final String user;
  private final boolean skipAnon;
  private final boolean summary;

  private ReqInOutLogEntry lastMapRs;

  private final List<String> skipClasses =
          Arrays.asList("org.apache.struts2",
                        "org.bedework.timezones.server.",
                        "org.bedework.util.servlet.HttpServletUtils");

  private final List<String> skipUnparsed = Arrays.asList(
          "A soft-locked cache entry");

  private final List<String> skipContent = Arrays.asList(
          "About to embed ",
          "About to flush",
          "About to get state",
          "About to prepare render",
          "After embed ",
          "Close for ",
          "contentlen=",
          "entry",
          "Found form in session",
          "getUserPrincipal.name",
          "getWriter called",
          "host=",
          "HttpUtils.getRequestURL(req) = ",
          "java.sql.Connection#beginRequest has been invoked",
          "java.sql.Connection#endRequest has been invoked",
          "No form in session",
          "Obtained state",
          "out Obtained BwCallback object",
          "request=org.apache.struts2",
          "Request out for module ",
          "Request parameters - global info and uris",
          "Set presentation state",
          "Setting locale to ",
          "XSLTFilter: Converting",
          "==============",
          "parameters:",
          "actionType:",
          "conversation: ",
          "query=b=de",
          "query=null");

  private final List<String> skipForSummary = Arrays.asList(
          "checkSvci",
          "About to claim",
          "Begin transaction for ",
          "ChangeTable",
          "Check access for ",
          "Client interface --",
          "close Obtained BwCallback object",
          "current change token",
          "Date=",
          "Emitted:",
          "end ChangeTable",
          "End transaction for",
          "Event duration=",
          "Fetch collection with",
          "fetchChildren for",
          "fetchEntities: ",
          "flush for ",
          "Get Calendar home for",
          "Get event ",
          "getState--",
          "getUserEntry for",
          "handleException called",
          "Indexing to index",
          "IndexResponse:",
          "New hibernate session",
          "No access",
          "No messages emitted",
          "Not found",
          "offset:",
          "Open session for",
          "Return ok - access ok",
          "Search:",
          "Set event with location",
          "The size was"
  );

  public DisplaySessions(final String taskId,
                         final String user,
                         final boolean skipAnon,
                         final boolean summary) {
    this.taskId = taskId;
    this.user = user;
    this.skipAnon = skipAnon;
    this.summary = summary;
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
          if (lastMapRs.lastAdded != null) {
            lastMapRs.lastAdded.addLogEntry(le);
          } else {
            lastMapRs.addLogEntry(le);
          }
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

    if ("ERROR".equals(le.level)) {
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

    ReqInOutLogEntry mapRs = tasks.get(le.taskId);

    if (mapRs == null) {
      // No associated request - create a placeholder
      mapRs = ReqInOutLogEntry.forMissingEntry(le);
      tasks.put(le.taskId, mapRs);
    }
    mapRs.doingCalsuite = false;
    lastMapRs = mapRs;

    if (mapRs.skipping) {
      return;
    }

    final String lt = le.logText;

    // ======================== Request parameters ========
    if (mapRs.doingReqPars) {
      if (lt.startsWith("  ")) {
        if (lt.startsWith("  b = \"de\"")) {
          return;
        }
        if (mapRs.lastAdded != null) {
          mapRs.lastAdded.addLogEntry(le);
          return;
        }
      }

      mapRs.doingReqPars = false;
    }

    // getRemoteUser = bnjones-admin

    final String exitTo = "exit to ";
    final String gru = "getRemoteUser = ";
    final String gruri = "getRequestURI = ";
    final String grsess = "getRequestedSessionId = ";
    final String fcs = "Found calSuite BwCalSuiteWrapper";
    final String rq = "Request parameters";

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
    } else if (lt.startsWith(exitTo)) {
      mapRs.exitTo = lt.substring(exitTo.length());
    } else if (lt.startsWith(fcs)) {
      mapRs.doingCalsuite = true;
    } else if (rq.equals(lt)) {
      mapRs.doingReqPars = true;
      mapRs.addLogEntry(le);
      return;
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
    if (rsin.placeHolder) {
      out("   **** No REQUEST in found *****");
    }

    outFmt(" exit to: %s", rsin.exitTo);

    outFmt("   class: %s", rsin.className);
    outFmt("     uri: %s", rsin.uri);
    outFmt("    user: %s", rsin.user);
    if (rsin.calsuiteName != null) {
      outFmt("calsuite: %s", rsin.calsuiteName);
    } else {
      outFmt("calsuite: %s", "NONE");
    }

    logEntries:
    for (final var le: rsin.entries) {
      final var lt = le.logText;
      if (lt == null) {
        continue;
      }

      if ("ERROR".equals(le.level)) {
        out("******************An error occurred");
      }

      if (summary) {
        for (final var s: skipForSummary) {
          if (le.logText.startsWith(s)) {
            continue logEntries;
          }
        }
      }

      final var doingRpars = lt.equals("Request parameters");

      if (doingRpars) {
        if (!le.entries.isEmpty()) {
          out("  Request parameters:");
        } else {
          out("  Request parameters: none");
        }
      } else {
        outFmt("         %s", lt);
      }

      if (!le.entries.isEmpty()) {
        for (final var suble: le.entries) {
          outFmt("             %s", suble.logText);
        }
      }
    }

    out("----------------------------------\n");
  }

  private boolean startMatches(final String text,
                               final Set<String> prefixes) {
    for (final var s: prefixes) {
      if (text.startsWith(s)) {
        return true;
      }
    }

    return false;
  }
}
