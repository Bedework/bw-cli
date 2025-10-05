/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd;

import org.bedework.base.response.GetEntitiesResponse;
import org.bedework.base.response.GetEntityResponse;
import org.bedework.base.response.Response;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.Collection;

/**
 * User: mike Date: 2/14/23 Time: 18:25
 */
public interface PicoCmdI extends Runnable {
  default void doExecute() {
    getOut().println(new CommandLine(this).getUsageMessage());
  }

  default void run() {
    doExecute();
  }

  String getLine();

  PrintWriter getOut();

  JolokiaConfigClient client();

  HttpClient getCl();

  default void multiLine(final GetEntitiesResponse<String> resp) {
    if (!resp.isOk()) {
      info("Failed response:" + resp);
      return;
    }

    multiLine(resp.getEntities());
  }

  default void multiLine(final Collection<String> resp) {
    if (resp == null) {
      info("Null response");
      return;
    }

    for (final String s: resp) {
      info(s);
    }
  }

  default boolean check(final Response<?> resp) {
    if (!resp.isOk()) {
      info("Failed response:" + resp);
      return false;
    }

    return true;
  }

  default void info(final Throwable t) {
    t.printStackTrace(getOut());
  }

  default void info(final GetEntityResponse<String> resp) {
    if (!resp.isOk()) {
      info("Failed response:" + resp);
      return;
    }

    info(resp.getEntity());
  }

  default void info(final String msg) {
    if (msg.endsWith("\n")) {
      getOut().print(msg);
    } else {
      getOut().println(msg);
    }
  }
}
