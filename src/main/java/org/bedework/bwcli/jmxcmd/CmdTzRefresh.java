/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdTzRefresh extends JmxCmd {
  public CmdTzRefresh() {
    super("tzrefresh", null, "Refresh the timezone data");
  }

  public void doExecute() throws Throwable {
    info(jcc.tzRefreshData());
  }
}
