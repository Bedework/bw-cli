/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "rebuild",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Rebuild the indexes."})
public class CmdRebuildIdx extends PicoCmd {
  @Parameters(index = "0",
          description = {"specify type of index"}, arity = "0..1")
  private String type;

  public void doExecute() throws Throwable {
    if (type == null) {
      multiLine(client().rebuildIndexes());
      return;
    }

    multiLine(client().rebuildEntityIndex(type));
  }
}
