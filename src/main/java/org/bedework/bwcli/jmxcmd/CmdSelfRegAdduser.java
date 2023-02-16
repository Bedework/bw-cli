/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@CommandLine.Command(name = "sradd",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Create user in selfreg db"})
public class CmdSelfRegAdduser extends PicoCmd {
  @CommandLine.Parameters(index = "0",
          paramLabel = "<account>",
          description = {"account id"}, arity = "1")
  private String account;

  @CommandLine.Parameters(index = "1",
          paramLabel = "<first-name>",
          description = {"first name of user"}, arity = "1")
  private String first;

  @CommandLine.Parameters(index = "2",
          paramLabel = "<last-name>",
          description = {"last name of user"}, arity = "1")
  private String last;

  @CommandLine.Parameters(index = "3",
          paramLabel = "<email>",
          description = {"email of user"}, arity = "1")
  private String email;

  @CommandLine.Parameters(index = "4",
          paramLabel = "<pw>",
          description = {"pw for account"}, arity = "1")
  private String pw;


  public void doExecute() throws Throwable {
    info(client().selfregAddUser(account, first, last, email, pw));
  }
}
