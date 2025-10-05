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
@CommandLine.Command(name = "dumpCal",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Dump the calendar data to the supplied ",
                "data path. The path must be ",
                "reachable by the server and will be set as the ",
                "input data path."})
public class CmdDumpCalData extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<path>",
          description = {"path to data"}, arity = "0..1")
  private String path;

  public void doExecute() {
    multiLine(client().dumpCalData(path));
  }
}
