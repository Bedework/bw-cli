/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.BwShell;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "makeidxprod",
        mixinStandardHelpOptions = true, version = "1.0",
        description = {
                "Move the prod alias for the given index."})
public class CmdMakeIdxProd extends JmxCmd implements Runnable {
  @Parameters(index = "0",
          description = {"index name | \"all\""})
  private String indexName;

  public CmdMakeIdxProd() {
    super("makeidxprod", "indexName | \"all\"",
          "Move the prod alias to the given index");
  }

  @CommandLine.ParentCommand
  BwShell.CliCommands parent;

  public void run() {
    try {
      if ("all".equals(indexName)) {
        parent.getOut().println(
                parent.getClient().makeAllIdxProd());
        return;
      }
      parent.getOut().println(
              parent.getClient().makeIdxProd(indexName));
    } catch (final Throwable t) {
      t.printStackTrace(parent.getOut());
    }
  }

  public void doExecute() throws Throwable {
    final String indexName = cli.word("indexName");

    if ("all".equals(indexName)) {
      info(jcc.makeAllIdxProd());
      return;
    }
    info(jcc.makeIdxProd(indexName));
  }
}
