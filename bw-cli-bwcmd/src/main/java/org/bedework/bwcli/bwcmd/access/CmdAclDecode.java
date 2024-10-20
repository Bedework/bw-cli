/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd.access;

import org.bedework.access.Acl;
import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "decode",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Decode an acl."})
public class CmdAclDecode extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          description = {"specify encoded acl"}, arity = "1")
  private String encodedAcl;

  public void doExecute() throws Throwable {
    final Acl acl = Acl.decode(encodedAcl);

    info(acl.toString());
  }
}
