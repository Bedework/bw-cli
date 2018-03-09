/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdSelfRegAdduser extends JmxCmd {
  public CmdSelfRegAdduser() {
    super("sradd", "account first last pw",
          "Create user in selfreg db - all parameters quoted strings");
  }

  public void doExecute() throws Throwable {
    final String account = cli.string("account");
    final String first = cli.string("first");
    final String last = cli.string("last");
    final String email = cli.string("email");
    final String pw = cli.string("pw");
    info(jcc.selfregAddUser(account, first, last, email, pw));
  }
}
