/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.logs;

import org.bedework.util.misc.ToString;

/**
 * User: mike Date: 1/14/20 Time: 22:20
 */
public class LogEntry {
  private static long startMillis;
  private static long lastMillis;

  protected String req;
  protected int curPos; // while parsing

  public Long millis; // time converted to milliseconds
  public long sinceLastMillis;
  public long sinceStartMillis;
  public String dt;
  public String level;
  public String className;
  public String taskId;
  public String logText;

  public boolean unparsed;

  String logName;

  public void unparsed(final String req) {
    unparsed = true;
    logText = req;
  }

  /**
   * @param req log entry
   * @return position we reached or null for bad record
   */
  public Integer parse(final String req,
                       final String logName,
                       final String logLevel) {
    try {
      this.req = req;
      this.logName = logName;

      dt = req.substring(0, 23);
      curPos = req.indexOf(" ", 24);
      level = req.substring(24, curPos);

      if ((logLevel != null) && !logLevel.equals(level)) {
        return null;
      }

      millis = millis();
      if (millis == null) {
        error("Unable to get millis for %s", req);
        return null;
      }

      if (startMillis != 0) {
        sinceLastMillis = millis - lastMillis;
        sinceStartMillis = millis - startMillis;
      } else {
        startMillis = millis;
      }

      lastMillis = millis;

      className = className(req);
      taskId = taskId(req);

      if (posValid()) {
        logText = req.substring(curPos);
      } else {
        logText = "";
      }

      if (logName == null) {
        return curPos;
      }

      curPos = req.indexOf(logName + ":");

      if (curPos < 0) {
        error("No name found for %s", req);
        return null;
      }

      curPos += logName.length() + 2; // skip ":"

      //if (!logName.equals(field())) {
      //  error("Expected %s for %s", logName, req);
      //  return null;
      //}

      return curPos;
    } catch (final Throwable t) {
      error("Unable to parse %s", req);
      throw t;
    }
  }

  public boolean sameTask(final LogEntry otherEntry) {
    if (!taskId.equals(otherEntry.taskId)) {
      out("taskId mismatch");
      return false;
    }

    return true;
  }

  public Long millis() {
    try {
      // 2019-01-04 00:00:11,742 ...
      // 0123456789012345678901234

      final long hrs = Integer.parseInt(req.substring(11, 13));
      final long mins = Integer.parseInt(req.substring(14, 16));
      final long secs = Integer.parseInt(req.substring(17, 19));
      final long millis = Integer.parseInt(req.substring(20, 23));

      return ((((hrs * 60) + mins) * 60) + secs) * 1000 + millis;
    } catch (final Throwable ignored) {
      return null;
    }
  }

  protected boolean posValid() {
    return (curPos >= 0) && (curPos < req.length());
  }

  private String className(final String ln) {
    int cnPos = ln.indexOf("[");
    if (cnPos < 0) {
      return null;
    }

    cnPos ++;

    curPos = ln.indexOf("]", cnPos);

    if (curPos < 0) {
      return null;
    }

    final var res = ln.substring(cnPos, curPos);

    curPos++;

    return res;
  }

  private String taskId(final String ln) {
    //final int taskIdPos = ln.indexOf("] (default");
    int taskIdPos = ln.indexOf("] (");
    if (taskIdPos < 0) {
      return null;
    }

    taskIdPos += 3;

    curPos = ln.indexOf(")", taskIdPos);

    if (curPos < 0) {
      return null;
    }

    final var res = ln.substring(taskIdPos, curPos);

    curPos += 2; // skip blank after taskid

    return res;
  }

  protected void error(final String format, final Object... args) {
    System.out.println(String.format(format, args));
  }

  protected void out(final String format, final Object... args) {
    System.out.println(String.format(format, args));
  }

  protected void toStringSegment(final ToString ts) {
    ts.append("taskId", taskId);
  }

  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
