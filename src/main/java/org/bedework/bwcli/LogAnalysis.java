/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
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
 * User: mike Date: 1/4/19 Time: 22:43
 */
public class LogAnalysis {
  long totalRequests;
  long totalForwardedRequests;
  long errorLines;
  long unterminatedTask;
  boolean showLong;

  final String wildflyStart = "[org.jboss.as] (Controller Boot Thread) WFLYSRV0025";

  class ReqStart {
    String taskId;
    String context;
    Long millis;
    String request;
    String dt;
  }

  final Map<String, Integer> ipMap = new HashMap<>();
  final Map<String, ReqStart> tasks = new HashMap<>();

  final static int numMilliBuckets = 20;
  final static int milliBucketSize = 100;

  class ContextInfo {
    String context;
    long requests;
    long totalMillis;
    long[] buckets = new long[numMilliBuckets];
    long rTotalReq; // Used for output

    ContextInfo(final String context) {
      this.context = context;
    }

    void reqOut(final String ln,
                final ReqStart rs,
                final long millis) {
      requests++;
      totalMillis += millis;

      int bucket = (int)(millis / milliBucketSize);

      if (bucket >= (numMilliBuckets - 1)) {
        bucket = numMilliBuckets - 1;
        if (showLong) {
          final String dt = ln.substring(0, ln.indexOf(" INFO"));

          out("Long request %s %d: %s - %s %s",
              rs.taskId, millis, rs.dt, dt, rs.request);
        }
      }

      buckets[bucket]++;
    }
  }

  final Map<String, ContextInfo> contexts = new HashMap<>();

  public boolean process(final String logPathName,
                         final boolean showLong) {
    this.showLong = showLong;

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
    final Long millis = millis(s);
    final String taskId = taskId(s);

    if (s.contains(wildflyStart)) {
      // Wildfly restarted
      tasks.clear();
      return;
    }

    //if (s.startsWith("2019-01-08 09:00:44,209")) {
    //  out("the line");
    //}

    final ReqStart rs = tryRequestLine(s);

    if (rs != null) {
      if (taskId == null)  {
        out("No taskid %s", s);
        return;
      }

      rs.taskId = taskId;
      rs.millis = millis;

      final ReqStart mapRs = tasks.get(taskId);

      if (mapRs != null) {
        // No request-out message
        unterminatedTask++;
      }

      tasks.put(taskId, rs);

      return;
    }

    if (isRequestOut(s)) {
      final ReqStart mapRs = tasks.get(taskId);

      if (mapRs == null) {
        final String dt = s.substring(0, s.indexOf(" INFO"));

        out("Missing taskid %s %s",
            taskId, dt);
        return;
      }

      if (mapRs.millis == null) {
        tasks.remove(taskId);
        return;
      }

      final long reqMillis = millis - mapRs.millis;

      ContextInfo ci =
              contexts.computeIfAbsent(mapRs.context,
                                       k -> new ContextInfo(mapRs.context));

      ci.reqOut(s, mapRs, reqMillis);

      // Done with the entry
      tasks.remove(taskId);
    }
  }

  private ReqStart tryRequestLine(final String ln) throws Throwable {
    if (!ln.contains(" REQUEST:")) {
      return null;
    }

    final ReqStart rs = new ReqStart();

    rs.dt = ln.substring(0, ln.indexOf(" INFO"));

    totalRequests++;

    String ip = null;
    int endIp = -1;

    final int charsetPos = ln.indexOf(":charset=");
    if (charsetPos > 0) {
      // Use to locate ip
      final int nextColonPos = ln.indexOf(":", charsetPos + 1);
      if (nextColonPos > 0) {
        // Unfortunately ipv6 addresses have a ":" separator
        endIp = ln.indexOf(":http", nextColonPos + 1);
        if (endIp > 0) {
          ip = ln.substring(nextColonPos + 1, endIp);
        }
      }
    }

    if (endIp < 0) {
      return null;
    }

    final int xffPos = ln.indexOf(" X-Forwarded-For:");

    if (xffPos > 0) {
      totalForwardedRequests++;

      // I think it's always the last field
      final String xff = ln.substring(xffPos + 17);

      if (!xff.equals("NONE")) {
        ip = xff;
      }
    }

    if (ip != null) {
      Integer ct = ipMap.computeIfAbsent(ip, k -> 0);

      ct = ct + 1;
      ipMap.put(ip, ct);
    }

    int urlPos = endIp + 10; // safely past the "//"

    final int reqPos = urlPos;

    urlPos = ln.indexOf("/", urlPos);
    if (urlPos < 0) {
      return null;
    }

    urlPos++; // past the /

    final int endContextPos = ln.indexOf("/", urlPos);
    if (endContextPos < 0) {
      return null;
    }

    final int endReqPos = ln.indexOf(" - ", endContextPos);

    try {
      rs.context = ln.substring(urlPos, endContextPos);
      rs.request = ln.substring(reqPos, endReqPos);

      return rs;
    } catch (final Throwable t) {
      out("%s", ln);
      out("%s: %s: %s: %s ",
          urlPos, endContextPos,
          reqPos, endReqPos);

      throw t;
    }
  }

  private String taskId(final String ln) throws Throwable {
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

  private boolean isRequestOut(final String ln) throws Throwable {
    return ln.indexOf(" REQUEST-OUT:") > 0;
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

  public Long millis(final String ln) {
    try {
      // 2019-01-04 00:00:11,742 ...
      // 0123456789012345678901234

      final long hrs = Integer.valueOf(ln.substring(11, 13));
      final long mins = Integer.valueOf(ln.substring(14, 16));
      final long secs = Integer.valueOf(ln.substring(17, 19));
      final long millis = Integer.valueOf(ln.substring(20, 23));

      return ((((hrs * 60) + mins) * 60) + secs) * 1000 + millis;
    } catch (final Throwable ignored) {
      return null;
    }
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

    final StringBuilder totReq =
            new StringBuilder(String.format(labelPattern, "Total"));
    final StringBuilder avgMs =
            new StringBuilder(String.format(labelPattern, "Avg ms"));

    for (int j = 0; j < cis.length; j++) {
      final ContextInfo ci = cis[j];

      totReq.append(String.format(hdrFormats[j],
                                  ci.requests));
      avgMs.append(String.format(hdrFormats[j],
                                 (int)(ci.totalMillis / ci.requests)));
    }

    out("%s", totReq);
    out("%s", avgMs);
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

  private void out() {
    System.out.println();
  }
}
