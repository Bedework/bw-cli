/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.BwShell;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "listidx",
        mixinStandardHelpOptions = true, version = "1.0",
        description = {
        "List open search indexes."})
public class CmdListIdx extends JmxCmd implements Runnable {
  public CmdListIdx() {
    super("listidx", null, "List the indexes");
  }

  @ParentCommand
  BwShell.CliCommands parent;

  public void run() {
    try {
      parent.getOut().println(parent.getClient().listIndexes());
    } catch (final Throwable t) {
      t.printStackTrace(parent.getOut());
    }
  }

  public void doExecute() throws Throwable {
    info(jcc.listIndexes());
  }
}
