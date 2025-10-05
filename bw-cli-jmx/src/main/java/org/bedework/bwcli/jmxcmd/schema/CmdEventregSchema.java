/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.schema;

import org.bedework.bwcli.jmxcmd.SchemaCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
@CommandLine.Command(name = "evreg",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create the eventreg schema"})
public class CmdEventregSchema extends SchemaCmd {
  public void doExecute() {
    multiLine(client().eventregSchema(export, out));
  }
}
