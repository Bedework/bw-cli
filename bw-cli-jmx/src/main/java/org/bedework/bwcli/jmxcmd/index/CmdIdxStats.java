/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@CommandLine.Command(name = "stats",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Get the index stats."})
public class CmdIdxStats extends PicoCmd {
  @Parameters(index = "0",
          description = {"specify index name"}, arity = "1")
  private String indexName;

  public void doExecute() throws Throwable {
    info(client().indexStats(indexName).toString());
  }
}
