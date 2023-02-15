/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

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
  public PrintWriter getOut() {
    return parent.getOut();
  }

  public JolokiaConfigClient client() {
    return parent.client();
  }
}
