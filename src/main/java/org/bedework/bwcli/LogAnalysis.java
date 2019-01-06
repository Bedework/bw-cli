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

        final Long millis = millis(s);
        final String taskId = taskId(s);

        if (infoLine(s) && s.contains(wildflyStart)) {
          // Wildfly restarted
          tasks.clear();
          continue;
        }

        final ReqStart rs = tryRequestLine(s);

        if (rs != null) {
          if (taskId == null)  {
            continue;
          }

          rs.taskId = taskId;
          rs.millis = millis;

          final ReqStart mapRs = tasks.get(taskId);

          if (mapRs != null) {
            // No posttransform message
            unterminatedTask++;
            continue;
          }

          tasks.put(taskId, rs);

          continue;
        }

        if (tryErrorLine(s)) {
          continue;
        }

        if (isRequestOut(s)) {
          final ReqStart mapRs = tasks.get(taskId);

          if (mapRs == null) {
            final String dt = s.substring(0, s.indexOf(" INFO"));

            out("Missing taskid %s %s",
                taskId, dt);
            continue;
          }

          if (mapRs.millis == null) {
            tasks.remove(taskId);
            continue;
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

      results();

      return true;
    } catch (final Throwable t) {
      t.printStackTrace();
      return false;
    }
  }

  private ReqStart tryRequestLine(final String ln) throws Throwable {
    if (!infoLine(ln)) {
      return null;
    }

    if (!ln.contains(" REQUEST:")) {
      return null;
    }

    final ReqStart rs = new ReqStart();

    rs.dt = ln.substring(0, ln.indexOf(" INFO"));

    totalRequests++;

    String ip = null;

    final int charsetPos = ln.indexOf(":charset=");
    if (charsetPos > 0) {
      // Use to locate ip
      final int nextColonPos = ln.indexOf(":", charsetPos + 1);
      if (nextColonPos > 0) {
        // Unfortunately ipv6 addresses have a ":" separator
        final int endColonPos = ln.indexOf(":http", nextColonPos + 1);
        if (endColonPos > 0) {
          ip = ln.substring(nextColonPos + 1, endColonPos);
        }
      }
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

    int urlPos = ln.indexOf("https://");
    if (urlPos < 0) {
      urlPos = ln.indexOf("http://");
      if (urlPos < 0) {
        return null;
      }
    }

    final int reqPos = urlPos;

    urlPos += 9;
    urlPos = ln.indexOf("/", urlPos);
    if (urlPos < 0) {
      return null;
    }

    urlPos++; // past the /

    final int endContextPos = ln.indexOf("/", urlPos);
    if (endContextPos < 0) {
      return null;
    }

    final int endReqPos = ln.indexOf(" - ");

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

  private void out(final String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  private void out() {
    System.out.println();
  }

  private String taskId(final String ln) throws Throwable {
    if (!infoLine(ln)) {
      return null;
    }

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

  private boolean isPostTransform(final String ln) throws Throwable {
    if (!infoLine(ln)) {
      return false;
    }

    return ln.indexOf(" POSTTRANSFORM:") > 0;
  }

  private boolean isRequestOut(final String ln) throws Throwable {
    if (!infoLine(ln)) {
      return false;
    }

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

  public <K, V extends Comparable> List<Map.Entry<K, V>> sortMap(Map<K, V> map) {
    // We create a list from the elements of the unsorted map
    List <Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());

    // Now sort the list
    Comparator<Map.Entry<K, V>> comparator =
            Comparator.comparing(Map.Entry<K, V>::getValue);
    list.sort(comparator.reversed());

    return list;
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

    final String pattern = "%15s";

    final StringBuilder sb =
            new StringBuilder(String.format(pattern, ""));

    sb.append(" \tTotal \tAvg ms");

    for (int i = 0; i < numMilliBuckets; i++) {
      sb.append(" \t");
      sb.append("<");
      sb.append((i + 1) * 100);
    }

    out("%s", sb);

    for (final String context: contextNames) {
      final ContextInfo ci = contexts.get(context);

      final StringBuilder l =
              new StringBuilder(String.format(pattern, context));

      l.append(" \t");
      l.append(ci.requests);
      l.append(" \t");
      l.append((int)(ci.totalMillis / ci.requests));

      final StringBuilder percents =
              new StringBuilder(String.format(pattern, ""));

      percents.append(" \t \t");

      long rTotalReq = 0;

      for (int i = 0; i < numMilliBuckets; i++) {
        l.append(" \t");
        l.append(ci.buckets[i]);

        rTotalReq += ci.buckets[i];
        percents.append(" \t");
        percents.append((int)(100 * rTotalReq / ci.requests));
        percents.append("%");
      }

      out("%s", l);
      out("%s", percents);
    }

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
}
