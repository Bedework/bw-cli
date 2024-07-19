/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd;

import org.bedework.bwcli.bwcmd.HttpClient;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.List;

/**
 * User: mike Date: 2/14/23 Time: 18:25
 */
public interface PicoCmdI extends Runnable {
  default void doExecute() throws Throwable {
    getOut().println(new CommandLine(this).getUsageMessage());
  }

  default void run() {
    try {
      doExecute();
    } catch (final Throwable t) {
      t.printStackTrace(getOut());
    }
  }

  String getLine();

  PrintWriter getOut();

  JolokiaConfigClient client();

  HttpClient getCl();

  default void multiLine(final List<String> resp) {
    if (resp == null) {
      info("Null response");
      return;
    }

    for (final String s: resp) {
      info(s);
    }
  }

  default void info(final String msg) {
    if (msg.endsWith("\n")) {
      getOut().print(msg);
    } else {
      getOut().println(msg);
    }
  }
}
