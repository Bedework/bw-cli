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
@Command(name = "sync",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class,
                        CmdGetAttr.class,
                        CmdResched.class,
                        CmdSetAttr.class,
                        CmdStart.class,
                        CmdStop.class,
        },
        description = {
                "Synch engine commands."})
public class CmdSync extends PicoCmd {
}
