/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.logs;

import org.bedework.util.misc.ToString;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: mike Date: 1/14/20 Time: 22:27
 */
public class ReqInOutLogEntry extends LogEntry {
  public static final Map<String, Integer> ipMap = new HashMap<>();

  public String ip;

  String url;

  boolean unparseable;

  List<NameValuePair> params;

  public String context;
  public String request;

  String sessionId;

  String logPrefix;

  String charset;

  String referer;
  String xForwardedFor;

  public Integer parse(final String req,
                       final boolean in) {
    final String logName;
    if (in) {
      logName = "REQUEST";
    } else {
      logName = "REQUEST-OUT";
    }

    if (!req.contains(" " + logName + ":") ||
            (super.parse(req, logName, "INFO") == null)) {
      return null;
    }

    sessionId = field();
    if (sessionId == null) {
      error("No session end found for %s", req);
      return null;
    }

    logPrefix = field();

    charset = field();
    if (charset == null) {
      error("No charset found for %s", req);
      return null;
    }

    ip = field("http");
    if (ip == null) {
      error("No ip for %s", req);
      return null;
    }

    url = dashField();
    if (url == null) {
      error("No url for %s", req);
      return null;
    }

    String fname = field();
    if (!"Referer".equals(fname)) {
      error("Expected referer for %s", req);
      return null;
    }

    referer = dashField();

    fname = field();
    if (!"X-Forwarded-For".equals(fname)) {
      error("Expected X-Forwarded-For for %s", req);
      return null;
    }

    // I think it's always the last field
    xForwardedFor = req.substring(curPos);

    if (!xForwardedFor.equals("NONE")) {
      ip = xForwardedFor;
    }

    if (in) {
      int ct = ipMap.computeIfAbsent(ip, k -> 0);

      ct = ct + 1;
      ipMap.put(ip, ct);
    }

    // Parse out the url
    int urlPos = 10; // safely past the "//"

    final int reqPos = urlPos;

    urlPos = url.indexOf("/", urlPos);
    if (urlPos < 0) {
      // No context
      return curPos;
    }

    urlPos++; // past the /

    final int endContextPos = url.indexOf("/", urlPos);
    if (endContextPos < 0) {
      return curPos;
    }

    try {
      context = url.substring(urlPos, endContextPos);
      request = url.substring(reqPos);

      if (context.trim().length() == 0) {
        context = null;
      }
    } catch (final Throwable t) {
      out("%s", req);
      out("%s: %s: %s ",
          urlPos, endContextPos,
          reqPos);
      return null;
    }

    try {
      params = URLEncodedUtils.parse(new URI(url), "UTF-8");
      unparseable = false;
    } catch (final Throwable ignored) {
      unparseable = true;
    }

    return curPos;
  }

  public boolean sameTask(final ReqInOutLogEntry otherEntry) {
    if (!super.sameTask(otherEntry)) {
      return false;
    }

    if (!sessionId.equals(otherEntry.sessionId)) {
      out("sessionId mismatch");
      return false;
    }

      /* These 2 don't always match -- why?
      if (!logPrefix.equals(otherEntry.logPrefix)) {
        out("logPrefix mismatch");
        return false;
      }

      if (!charset.equals(otherEntry.charset)) {
        out("charset mismatch");
        return false;
      }
      */

    if (!ip.equals(otherEntry.ip)) {
      out("ip mismatch");
      return false;
    }

      /* url will not match because we may have been redirected (to
         a jsp page)
      if (!url.equals(otherEntry.url)) {
        out("url mismatch");
        return false;
      }
       */

    return true;
  }

  public boolean hasJsessionid() {
    return (url != null) && url.contains(";jsessionid=");
  }

  // Expect this next
  protected String field() {
    return field("");
  }

  // Needed because ipv6 addresses have ':'
  protected String field(final String nextFieldStart) {
    final int start = curPos;
    final int end = req.indexOf(":" + nextFieldStart, start);
    if (end < 0) {
      error("No end found for %s", req);
      return null;
    }

    final String res = req.substring(start, end);
    curPos = end + 1; // Skip only the ":"

    return res;
  }

  String dashField() {
    final int start = curPos;
    final int end = req.indexOf(" - ", start);
    if (end < 0) {
      error("No request found for %s", req);
      return null;
    }

    final String res = req.substring(start, end);
    curPos = end + 3;

    return res;
  }

  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("sessionId", sessionId);
    ts.append("logPrefix", logPrefix);
    ts.append("charset", charset);
    ts.append("ip", ip);
    ts.append("url", url);
  }
}
