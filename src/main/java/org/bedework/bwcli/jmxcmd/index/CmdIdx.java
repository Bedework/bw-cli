/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "idx",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class,
                        CmdIdxStats.class,
                        CmdListIdx.class,
                        CmdMakeIdxProd.class,
                        CmdNewidx.class,
                        CmdPurgeIdx.class,
                        CmdRebuildIdx.class,
                        CmdRebuildStatus.class,
                        CmdReindex.class,
        },
        description = {
                "Index manipulation commands."})
public class CmdIdx extends PicoCmd {
}
