/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.LogEntry;
import org.bedework.bwcli.logs.ReqInOutLogEntry;

/**
 * User: mike Date: 10/23/22 Time: 10:53
 */
public class SummariseTests extends LogAnalysis {
  int waitcountCount;

  LogEntry lastEntry;

  public void processRecord(final String s) {
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
    final var isCalTest = s.contains(testUserAgentLabel);
    final var isWaitcount = isCalTest && s.contains("WAITCOUNT ");

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
      final var pos = le.logText.indexOf(testUserAgentLabel);
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

  public void processInfo(final ReqInOutLogEntry rs) {
    if (waitcountCount <= 1) {
      outSummary(rs);
    }
  }

  @Override
  public void results() {

  }

  private void outSchedSummary(final LogEntry le) {
    final var s = le.logText;

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
}
