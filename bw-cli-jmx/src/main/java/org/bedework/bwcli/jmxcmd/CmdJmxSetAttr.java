/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@CommandLine.Command(name = "setattr",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Set the named attribute in the named mbean"})
public class CmdJmxSetAttr extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          description = {"specify mbean name"}, arity = "1")
  private String mbean;

  @CommandLine.Parameters(index = "1",
          description = {"specify attribute name"}, arity = "1")
  private String attrname;

  @CommandLine.Parameters(index = "2",
          description = {"specify attribute value"}, arity = "1")
  private String attrval;

  public void doExecute() throws Throwable {
    client().setAttr(mbean, attrname, attrval);
  }
}
