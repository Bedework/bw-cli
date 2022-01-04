/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
public class CmdNotifierSchema extends JmxCmd {
  public CmdNotifierSchema() {
    super("noteschema", "[export] [outfile]", "Create the calendar core schema");
  }

  public void doExecute() throws Throwable {
    multiLine(jcc.notifierSchema(cli.nextIs("export"),
                                 cli.optionalKeyString("out")));
  }
}
