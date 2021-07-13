/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdTzGetAttr extends JmxCmd {
  public CmdTzGetAttr() {
    super("tzgetattr", null, "Get a timezone server attribute");
  }

  public void doExecute() throws Throwable {
    final String attrname = cli.string("attrname");
    info(jcc.getTzAttr(attrname));
  }
}
