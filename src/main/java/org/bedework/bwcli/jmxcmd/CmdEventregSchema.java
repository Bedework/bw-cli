/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
public class CmdEventregSchema extends JmxCmd {
  public CmdEventregSchema() {
    super("eventregschema", null, "Create the eventreg schema");
  }

  public void doExecute() throws Throwable {
    multiLine(jcc.eventregSchema(cli.nextIs("export"),
                                 cli.optionalKeyString("out")));
  }
}
