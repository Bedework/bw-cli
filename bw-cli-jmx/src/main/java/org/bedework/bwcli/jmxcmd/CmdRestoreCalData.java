/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:48
 */
@CommandLine.Command(name = "restoreCal",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Restore the calendar data from the default or supplied ",
                "data path. If a path is supplied it must be ",
                "reachable by the server and will be set as the ",
                "input data path."})
public class CmdRestoreCalData extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<path>",
          description = {"path to data"}, arity = "0..1")
  private String path;

  public void doExecute() throws Throwable {
    multiLine(client().restoreCalData(path));
  }
}
