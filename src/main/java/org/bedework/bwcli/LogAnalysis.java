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
  long totalMillis;
  long unterminatedTask;

  class ReqStart {
    String taskId;
    String context;
    Long millis;
  }

  final Map<String, Integer> ipMap = new HashMap<>();
  final Map<String, ReqStart> tasks = new HashMap<>();

  final static int numMilliBuckets = 20;
  final static int milliBucketSize = 100;

  final Map<String, long[]> contextMilliBuckets = new HashMap<>();

  public boolean process(final String logPathName) {
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

        final String context = tryRequestLine(s);

        if (context != null) {
          if (taskId == null)  {
            continue;
          }

          final ReqStart rs = new ReqStart();

          rs.taskId = taskId;
          rs.millis = millis;
          rs.context = context;

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

        if (isPostTransform(s)) {
          final ReqStart rs = tasks.get(taskId);

          if (rs == null) {
            continue;
          }

          if (rs.millis == null) {
            tasks.remove(taskId);
            continue;
          }

          final long reqMillis = millis - rs.millis;

          totalMillis += reqMillis;
          final int bucket = Math.min(numMilliBuckets - 1,
                                      (int)(reqMillis / milliBucketSize));

          long[] milliBuckets =
                  contextMilliBuckets
                          .computeIfAbsent(rs.context,
                                           k -> new long[numMilliBuckets]);

          milliBuckets[bucket]++;
        }
      }

      System.out.println("Total requests: " + totalRequests);
      if (totalForwardedRequests != totalRequests) {
        System.out.println(
                "Total forwarded requests: " + totalForwardedRequests);
      }

      System.out.println("Total millis: " + totalMillis);
      System.out.println("Avg millis per request: " + totalMillis / totalRequests);

      System.out.println("Millis per request by context per 100 millis");

      final Set<String> contexts = new TreeSet<>(contextMilliBuckets.keySet());

      final String pattern = "%15s";

      final StringBuilder sb = new StringBuilder(String.format(pattern, ""));

      sb.append(" \tTotal");

      for (int i = 0; i < numMilliBuckets; i++) {
        sb.append(" \t");
        sb.append("<");
        sb.append((i + 1) * 100);
      }
      System.out.println(sb.toString());

      for (final String context: contexts) {
        long[] milliBuckets = contextMilliBuckets.get(context);

        final StringBuilder l =
                new StringBuilder(String.format(pattern, context));

        long total = 0;
        for (int i = 0; i < numMilliBuckets; i++) {
          total += milliBuckets[i];
        }

        l.append(" \t");
        l.append(total);

        for (int i = 0; i < numMilliBuckets; i++) {
          l.append(" \t");
          l.append(milliBuckets[i]);
        }

        System.out.println(l.toString());
      }
      System.out.println();

      System.out.println("Total error lines: " + errorLines);

      System.out.println();

      final List<Map.Entry<String, Integer>> sorted =
              sortMap(ipMap);
      int ct = 0;
      for (Map.Entry<String, Integer> ent: sorted) {
        System.out.println(ent.getKey() + "\t" + ent.getValue());
        ct++;

        if (ct > 20) {
          break;
        }
      }

      return true;
    } catch (final Throwable t) {
      t.printStackTrace();
      return false;
    }
  }

  private String tryRequestLine(final String ln) throws Throwable {
    if (ln.indexOf(" INFO ") != 23) {
      return null;
    }

    final int reqPos = ln.indexOf(" REQUEST:");

    if (reqPos < 0) {
      return null;
    }

    totalRequests++;

    final int xffPos = ln.indexOf(" X-Forwarded-For:");

    if (xffPos > 0) {
      totalForwardedRequests++;

      // I think it's always the last field
      final String ip = ln.substring(xffPos + 17);

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

    try {
      final String context = ln.substring(urlPos, endContextPos);

      return context;
    } catch (final Throwable t) {
      System.out.println(ln);
      System.out.println(" " + urlPos + ": " + endContextPos);

      throw t;
    }
  }

  private String taskId(final String ln) throws Throwable {
    if (ln.indexOf(" INFO ") != 23) {
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

    return ln.substring(taskIdPos, endTaskIdPos);
  }

  private boolean isPostTransform(final String ln) throws Throwable {
    if (ln.indexOf(" INFO ") != 23) {
      return false;
    }

    return ln.indexOf(") POSTTRANSFORM:") > 0;
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

}
