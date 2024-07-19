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
@CommandLine.Command(name = "cal",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create the calendar core schema"})
public class CmdCalSchema extends SchemaCmd {
  public void doExecute() throws Throwable {
    multiLine(client().coreSchema(export, out));
  }
}
