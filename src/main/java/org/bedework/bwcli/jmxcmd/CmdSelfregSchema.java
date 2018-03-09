/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
public class CmdSelfregSchema extends JmxCmd {
  public CmdSelfregSchema() {
    super("selfregschema", null, "Create the selfreg schema");
  }

  public void doExecute() throws Throwable {
    multiLine(jcc.selfregSchema());
  }
}
