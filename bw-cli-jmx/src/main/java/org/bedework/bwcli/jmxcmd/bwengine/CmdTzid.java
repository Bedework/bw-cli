/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.bwengine;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "tzid",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Set or get tzid."})
public class CmdTzid extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<tzid>",
          description = {"tzid for system"}, arity = "0..1")
  private String tzid;

  public void doExecute() throws Throwable {
    if (tzid != null) {
      client().setSystemTzid(tzid);
    }
    info(client().getSystemTzid());
  }
}
