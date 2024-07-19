/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd.index;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:43
 */
@CommandLine.Command(name = "new",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create new indexes."})
public class CmdNewidx extends PicoCmd {
  public void doExecute() throws Throwable {
    info(client().newIndexes());
  }
}
