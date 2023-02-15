/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:46
 */
@Command(name = "rebuildstatus",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Show status of index rebuild"})
public class CmdRebuildStatus extends PicoCmd {
  public void doExecute() throws Throwable {
    multiLine(client().rebuildIdxStatus());
  }
}
