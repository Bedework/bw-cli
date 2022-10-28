/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.LogEntry;
import org.bedework.bwcli.logs.ReqInOutLogEntry;

import java.util.Arrays;
import java.util.List;

import static org.bedework.bwcli.DisplaySessions.DisplayMode.list;
import static org.bedework.bwcli.DisplaySessions.DisplayMode.summary;

/**
 * User: mike Date: 10/23/22 Time: 21:53
 */
public class DisplaySessions extends LogAnalysis {
  private final String taskId;
  private final String sessionId;
  private final String user;
  private final String requestDt;
  private final boolean skipAnon;
  private final boolean displayTotals;

  public enum DisplayMode {
    full,    // display everything
    summary, // Small subset of entries
    list     // Enough to track requests
  }

  private final DisplayMode displayMode;

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
                         final String sessionId,
                         final String user,
                         final String requestDt,
                         final boolean skipAnon,
                         final boolean displayTotals,
                         final DisplayMode displayMode) {
    this.taskId = taskId;
    this.sessionId = sessionId;
    this.user = user;
    this.requestDt = requestDt;
    this.skipAnon = skipAnon;
    this.displayTotals = displayTotals;
    this.displayMode = displayMode;
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

    if (startMatches(le.className, skipClasses) ||
        startMatches(le.logText, skipContent)) {
      return;
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

    if (sessionId != null) {
      if ((mapRs.sessid != null) &&
            !mapRs.sessid.equals(sessionId)){
        mapRs.skipping = true;
        return;
      }
    }

    if (requestDt != null) {
      if ((mapRs.dt != null) &&
            !mapRs.dt.startsWith(requestDt)){
        mapRs.skipping = true;
        return;
      }
    }

    if (mapRs.skipping) {
      return;
    }

    if ("ERROR".equals(le.level)) {
      mapRs.hadError = true;
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


  public void results() {
    if (displayTotals) {
      super.results();
    }
  }

  final String sessionDelim = "----------------------------------\n";

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

    if (rsin.hadError) {
      out("***** An error occurred");
    }

    outFmt("  sessid: %s", rsin.sessid);
    outFmt("   class: %s", rsin.className);
    outFmt("     uri: %s", rsin.uri);
    outFmt("    user: %s  calsuite %s", rsin.user, rsin.calsuiteName);

    final var fetchEvent = rsin.className.endsWith("FetchEventAction");
    final var updateEvent = rsin.className.endsWith("UpdateEventAction");

    if ((displayMode == list) && !fetchEvent && !updateEvent) {
      out(sessionDelim);
      return;
    }

    logEntries:
    for (final var le: rsin.entries) {
      final var lt = le.logText;
      if (lt == null) {
        continue;
      }

      final var doingRpars = lt.equals("Request parameters");

      if (fetchEvent && doingRpars) {
        displayEventHref(le);
        continue logEntries;
      }

      if (doingRpars && updateEvent) {
        displayEventHref(le);
      }

      if (displayMode == list) {
        continue;
      }

      if ((displayMode == summary) &&
              startMatches(le.logText, skipForSummary)) {
        continue logEntries;
      }

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

    out(sessionDelim);
  }

  private void displayEventHref(final LogEntry le) {
    String calPath = null;
    String guid = null;
    String rid = null;
    String href = null;

    if (!le.entries.isEmpty()) {
      for (final var suble: le.entries) {
        var val = tryReqPar(suble, "calPath");
        if (val != null) {
          calPath = val;
          continue;
        }

        val = tryReqPar(suble, "guid");
        if (val != null) {
          guid = val;
          continue;
        }

        val = tryReqPar(suble, "recurrenceId");
        if (val != null) {
          rid = val;
          continue;
        }

        val = tryReqPar(suble, "href");
        if (val != null) {
          href = val;
        }
      }
    }

    if (href != null) {
      outFmt("   event: %s", href);
      return;
    }

    if (rid == null) {
      outFmt("   event: %s %s", calPath, guid);
      return;

    }

    outFmt("   event: %s %s %s", calPath, guid, rid);
  }

  private String tryReqPar(final LogEntry le,
                           final String parName) {
    var test = parName + " = \"";
    final var lt = le.logText;
    final var pos = lt.indexOf(test);

    if (pos < 0) {
      return null;
    }

    return lt.substring(pos + test.length()).replace("\"", "");
  }

  private boolean startMatches(final String text,
                               final List<String> prefixes) {
    if (text == null) {
      return false;
    }

    for (final var s: prefixes) {
      if (text.startsWith(s)) {
        return true;
      }
    }

    return false;
  }
}
