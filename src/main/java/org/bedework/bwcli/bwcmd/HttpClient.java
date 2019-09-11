/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd;

/*
 * User: mike
 * Date: 3/7/17
 * Time: 18:05
 */

import org.bedework.bwcli.JsonMapper;
import org.bedework.util.http.Headers;
import org.bedework.util.http.PooledHttpClient;
import org.bedework.util.misc.Util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Carry out all communications with web service
 *
 */
public class HttpClient extends PooledHttpClient {
  public HttpClient(final URI uri) throws Exception {
      super(uri,
            new JsonMapper());

      setHeadersFetcher(this::getHeaders);
  }

  /*
  public int post(final String path,
                  final Object val) throws Exception {
    try {
      final StringWriter sw = new StringWriter();
      om.writeValue(sw, val);

      return post(path, sw.toString());
    } catch (final Throwable t) {
      throw new Exception(t);
    } finally {
      try {
        http.release();
      } catch (final Throwable ignored) {}
    }
  }
   */

  private Headers getHeaders() {
    final Headers hdrs = new Headers();

    return hdrs;
  }

  private String encode(final String val) throws Throwable {
    return URLEncoder.encode(val, StandardCharsets.UTF_8);
  }

  private static class ReqBldr {
    final StringBuilder req = new StringBuilder();

    String delim = "?";

    ReqBldr(final String path) {
      req.append(path);
    }

    void par(final String name,
             final String value) {
      req.append(delim);
      delim = "&";
      req.append(name);
      req.append("=");
      req.append(value);
    }

    void par(final String name,
             final int value) {
      par(name, String.valueOf(value));
    }

    void par(final String name,
             final boolean value) {
      par(name, String.valueOf(value));
    }

    void multiPar(final String name,
                  final String[] value) throws Throwable {
      if ((value == null) || (value.length == 0)) {
        return;
      }

      for (final String s: value) {
        par(name, s);
      }
    }

    void multiPar(final String name,
                  final List<String> value) throws Throwable {
      if (Util.isEmpty(value)) {
        return;
      }

      for (final String s: value) {
        par(name, encode(s));
      }
    }

    void par(final String name,
             final List<String> value) throws Throwable {
      if (Util.isEmpty(value)) {
        return;
      }

      req.append(delim);
      delim = "&";
      req.append(name);
      req.append("=");

      String listDelim = "";

      final StringBuilder sb = new StringBuilder();
      for (final String s: value) {
        sb.append(listDelim);
        sb.append(s);
        listDelim = ",";
      }

      req.append(URLEncoder.encode(sb.toString(),
                                   StandardCharsets.UTF_8));
    }

    public String toString() {
      return req.toString();
    }

    private static String encode(final String val) throws Throwable {
      return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
  }
}
