/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.bwengine;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:46
 */
@CommandLine.Command(name = "sys",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class,
                        CmdAutokill.class,
                        CmdSysStats.class,
                        CmdTzid.class,
        },
        description = {
                "Set or display system settings."})
public class CmdSystem extends PicoCmd {
}
