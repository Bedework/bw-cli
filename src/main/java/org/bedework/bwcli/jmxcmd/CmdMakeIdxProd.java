/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdMakeIdxProd extends JmxCmd {
  public CmdMakeIdxProd() {
    super("makeidxprod", "indexName | \"all\"",
          "Move the prod alias to the given index");
  }

  public void doExecute() throws Throwable {
    final String indexName = cli.word("indexName");

    if ("all".equals(indexName)) {
      info(jcc.makeAllIdxProd());
      return;
    }
    info(jcc.makeIdxProd(indexName));
  }
}
