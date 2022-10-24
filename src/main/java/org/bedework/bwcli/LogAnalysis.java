/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.logs.ContextInfo;
import org.bedework.bwcli.logs.LogReader;
import org.bedework.bwcli.logs.ReqInOutLogEntry;
import org.bedework.util.misc.Util;

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
public class LogAnalysis extends LogReader {
  long totalRequests;
  long totalForwardedRequests;

  @Override
  public void processRecord(final String s) {
  }

  @Override
  public void processInfo(final ReqInOutLogEntry rs) {
  }

  public void results() {
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

    for (int i = 0; i < ContextInfo.numMilliBuckets; i++) {
      final StringBuilder l =
              new StringBuilder(String.format(labelPattern, "<" + ((i + 1) * 100)));

      for (int j = 0; j < cis.length; j++) {
        final ContextInfo ci = cis[j];
        // bucket and percent

        l.append(String.format(cellFormats[j],
                 ci.getBucket(i),
                 ((int)(100 * ci.rTotalReq / ci.getRequests()))));

        ci.rTotalReq += ci.getBucket(i);
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
                                  ci.getSessions()));
      totReq.append(String.format(hdrFormats[j],
                                  ci.getRequests()));
      avgMs.append(String.format(hdrFormats[j],
                                 (int)(ci.getTotalMillis() / ci.getRequests())));

      subTtotReq.append(String.format(hdrFormats[j],
                                      ci.getSubTrequests()));
      subTavgMs.append(String.format(hdrFormats[j],
                                     (int)(ci.getSubTtotalMillis() / ci.getSubTrequests())));
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
    for (final Map.Entry<String, Integer> ent: sorted) {
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
    for (final Map.Entry<String, Integer> ent: longSorted) {
      outFmt("%s\t%d", ent.getKey(), ent.getValue());
      ct++;

      if (ct > numIps) {
        break;
      }
    }
  }
}
