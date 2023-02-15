/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "makeprod",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Move the prod alias for the given index."})
public class CmdMakeIdxProd extends PicoCmd {
  @Option(names = {"all"},
          description = {"If present all indexes are made prod"})
  private boolean all;

  @Parameters(index = "0",
          description = {"specify index name"}, arity = "0..1")
  private String indexName;

  public CmdMakeIdxProd() {
  }

  public void doExecute() throws Throwable {
    if (all) {
      info(client().makeAllIdxProd());
      return;
    }
    info(client().makeIdxProd(indexName));
  }
}
