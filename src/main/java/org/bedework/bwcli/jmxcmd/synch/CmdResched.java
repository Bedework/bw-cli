/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.synch;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "resched",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Reschedule a subscription."})
public class CmdResched extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<id>",
          description = {"id of subscription"}, arity = "1")
  private String id;

  public void doExecute() throws Throwable {
    client().syncResched(id);
  }
}
