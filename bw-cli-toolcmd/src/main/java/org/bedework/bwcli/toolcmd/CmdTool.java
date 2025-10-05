/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.toolcmd;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:48
 */
@Command(name = "tool",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Execute a command on a remote tool."})
public class CmdTool extends PicoCmd {
  @Parameters(index = "0..*",
          description = {"command to execute"}, arity = "1")
  private String[] cmd;

  public void doExecute() {
    final String line = getLine();

    final int pos = line.indexOf("tool");
    info(client().execCmdutilCmd(line.substring(pos + 4)));
  }
}
