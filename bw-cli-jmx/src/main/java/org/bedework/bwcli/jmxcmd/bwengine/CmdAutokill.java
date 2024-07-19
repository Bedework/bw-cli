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
@Command(name = "autokill",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Set or get autokill delay in minutes."})
public class CmdAutokill extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<delay-minutes>",
          description = {"delay in minutes"}, arity = "0..1")
  private Integer delay;

  public void doExecute() throws Throwable {
    if (delay != null) {
      client().setAutoKillMinutes(delay);
    }
    info("Autokill delay = " + client().getAutoKillMinutes());
  }
}
