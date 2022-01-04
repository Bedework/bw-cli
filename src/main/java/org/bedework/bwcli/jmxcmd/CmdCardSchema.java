/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
public class CmdCardSchema extends JmxCmd {
  public CmdCardSchema() {
    super("cardschema", "[export] [outfile]", "Create the calendar core schema");
  }

  public void doExecute() throws Throwable {
    multiLine(jcc.carddavSchema(cli.nextIs("export"),
                                cli.optionalKeyString("out")));
  }
}
