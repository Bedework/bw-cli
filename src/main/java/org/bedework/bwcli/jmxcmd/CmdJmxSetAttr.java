/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdJmxSetAttr extends JmxCmd {
  public CmdJmxSetAttr() {
    super("setattr", "mbean attrname attrval",
          "Set the named attribute in the named mbean");
  }

  public void doExecute() throws Throwable {
    final String mbean = cli.string("mbean");
    final String attrname = cli.string("attrname");
    final String attrval = cli.string("attrval");
    jcc.setAttr(mbean, attrname, attrval);
  }
}
