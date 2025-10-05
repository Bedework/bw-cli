/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.bwengine;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@CommandLine.Command(name = "stats",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Get system statistics."})
public class CmdSysStats extends PicoCmd {
  public void doExecute() {
    multiLine(client().sysStats());
  }
}
