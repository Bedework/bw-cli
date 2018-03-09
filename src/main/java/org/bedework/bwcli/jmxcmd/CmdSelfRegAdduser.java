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
    final String account = cli.word("account");
    final String first = cli.word("first");
    final String last = cli.word("last");
    final String pw = cli.word("pw");
    info(jcc.selfregAddUser(account, first, last, pw));
  }
}
