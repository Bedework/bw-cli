/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.logs;

import java.util.Map;

/**
 * User: mike Date: 10/23/22 Time: 01:16
 */
public class ContextInfo {
  public final static int numMilliBuckets = 20;
  public final static int milliBucketSize = 100;

  String context;
  long requests;
  long totalMillis;

  // Total ignoring the highest bucket
  long subTrequests;
  long subTtotalMillis;

  long[] buckets = new long[numMilliBuckets];

  // How often we see ";jsessionid" in the incoming request
  long sessions;
  public long rTotalReq;

  final Map<String, Integer> longreqIpMap;

  public ContextInfo(final String context,
                     final Map<String, Integer> longreqIpMap) {
    this.context = context;
    this.longreqIpMap = longreqIpMap;
  }

  public void reqOut(final String ln,
                     final ReqInOutLogEntry rs,
                     final long millis,
                     final boolean showLong) {
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

  public long getBucket(final int i) {
    return buckets[i];
  }

  public long getRequests() {
    return requests;
  }

  public long getSessions() {
    return sessions;
  }

  public long getTotalMillis() {
    return totalMillis;
  }

  public long getSubTrequests() {
    return subTrequests;
  }

  public long getSubTtotalMillis() {
    return subTtotalMillis;
  }

  protected void outFmt(final String format,
                        final Object... args) {
    System.out.println(String.format(format, args));
  }
}
