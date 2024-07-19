/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.synch;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "getattr",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "get synch engine attribute."})
public class CmdGetAttr extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<attrname>",
          description = {"name of attribute"}, arity = "1")
  private String attrname;

  public void doExecute() throws Throwable {
    client().getSyncAttr(attrname);
  }
}
