/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.toolcmd;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:48
 */
@Command(name = "tooluser",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Set user for tools."})
public class CmdToolUser extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<account>",
          description = {"user account"}, arity = "1")
  private String account;

  public void doExecute() throws Throwable {
    info(client().setCmdutilUser(account));
  }
}
