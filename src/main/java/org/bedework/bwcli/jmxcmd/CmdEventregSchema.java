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
    final String exportKey = cli.word("[no]export");

    final boolean export;
    if ("export".equals(exportKey)) {
      export = true;
    } else if ("noexport".equals(exportKey)) {
      export = false;
    } else {
      cli.error("Expected 'export' or 'noexport'");
      return;
    }

    multiLine(jcc.eventregSchema(export));
  }
}
