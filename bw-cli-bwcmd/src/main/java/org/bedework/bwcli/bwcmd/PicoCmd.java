/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd;

import org.bedework.bwcli.bwcmd.HttpClient;

import picocli.CommandLine.ParentCommand;

import java.io.PrintWriter;

/**
 * User: mike Date: 2/14/23 Time: 18:25
 */
public abstract class PicoCmd implements PicoCmdI {
  @ParentCommand
  PicoCmdI parent;

  public void run() {
    try {
      doExecute();
    } catch (final Throwable t) {
      t.printStackTrace(parent.getOut());
    }
  }

  @Override
  public String getLine() {
    return parent.getLine();
  }

  @Override
  public PrintWriter getOut() {
    return parent.getOut();
  }

  public JolokiaConfigClient client() {
    return parent.client();
  }

  public HttpClient getCl() {
    return parent.getCl();
  }
}
