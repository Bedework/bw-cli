/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
@CommandLine.Command(name = "selfregschema",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create the selfreg schema"})
public class CmdSelfregSchema extends SchemaCmd {
  public void doExecute() throws Throwable {
    multiLine(client().selfregSchema(export, out));
  }
}
