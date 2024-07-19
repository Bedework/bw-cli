/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.synch;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "setattr",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "set synch engine attribute."})
public class CmdSetAttr extends PicoCmd {
  @Parameters(index = "0",
          paramLabel = "<attrname>",
          description = {"name of attribute"}, arity = "1")
  private String attrname;

  @Parameters(index = "1",
          paramLabel = "<value>",
          description = {"value of attribute"}, arity = "1")
  private String value;

  public void doExecute() throws Throwable {
    client().setSyncAttr(attrname, value);
  }
}
