/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdTzSetAttr extends JmxCmd {
  public CmdTzSetAttr() {
    super("tzsetattr", null, "Set a timezone server attribute");
  }

  public void doExecute() throws Throwable {
    final String attrname = cli.string("attrname");
    final String attrval = cli.string("attrval");
    jcc.setTzAttr(attrname, attrval);
  }
}
