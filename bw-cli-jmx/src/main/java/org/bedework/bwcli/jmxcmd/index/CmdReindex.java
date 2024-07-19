/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "reindex",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Reindex given doctype index into a new index"})
public class CmdReindex extends PicoCmd {
  @Parameters(index = "0",
          paramLabel = "<docType>",
          description = {"specify type of index"}, arity = "1")
  private String docType;
  public void doExecute() throws Throwable {
    info(client().reindex(docType));
  }
}
