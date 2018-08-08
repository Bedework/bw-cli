/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdSysStats extends JmxCmd {
  public CmdSysStats() {
    super("stats", null, "Get the system stats");
  }

  public void doExecute() throws Throwable {
    multiLine(jcc.sysStats());
  }
}
