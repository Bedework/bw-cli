/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdReindex extends JmxCmd {
  public CmdReindex() {
    super("reindex", "doctype",
          "Reindex current doctype index into a new index");
  }

  public void doExecute() throws Throwable {
    final String docType = cli.word("docType");
    info(jcc.reindex(docType));
  }
}
