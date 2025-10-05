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
@CommandLine.Command(name = "synch",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create the synch schema"})
public class CmdSynchSchema extends SchemaCmd {
  public void doExecute() {
    multiLine(client().syncSchema(export, out));
  }
}
