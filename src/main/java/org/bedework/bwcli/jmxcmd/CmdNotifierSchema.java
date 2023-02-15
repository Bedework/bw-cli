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
@CommandLine.Command(name = "noteschema",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create the notifier schema"})
public class CmdNotifierSchema extends SchemaCmd {
  public void doExecute() throws Throwable {
    multiLine(client().notifierSchema(export, out));
  }
}
