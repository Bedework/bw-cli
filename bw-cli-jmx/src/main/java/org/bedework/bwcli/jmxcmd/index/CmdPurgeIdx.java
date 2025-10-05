/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:43
 */
@CommandLine.Command(name = "purge",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Purge the old indexes."})
public class CmdPurgeIdx extends PicoCmd {
  public void doExecute() {
    info(client().purgeIndexes());
  }
}
