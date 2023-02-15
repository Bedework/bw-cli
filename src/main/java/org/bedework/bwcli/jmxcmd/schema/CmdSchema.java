/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.schema;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "schema",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class,
                        CmdCalSchema.class,
                        CmdCardSchema.class,
                        CmdEventregSchema.class,
                        CmdNotifierSchema.class,
                        CmdSelfregSchema.class,
                        CmdSynchSchema.class,
        },
        description = {
                "Schema manipulation commands."})
public class CmdSchema extends PicoCmd {
}
