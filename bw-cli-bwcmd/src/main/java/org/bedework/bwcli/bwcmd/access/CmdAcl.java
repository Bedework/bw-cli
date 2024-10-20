/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd.access;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "acl",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class,
                        CmdAclDecode.class },
        description = {
                "Decode an acl."})
public class CmdAcl extends PicoCmd {
}
