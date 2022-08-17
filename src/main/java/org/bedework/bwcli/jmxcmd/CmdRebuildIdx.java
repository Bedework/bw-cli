/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdRebuildIdx extends JmxCmd {
  public CmdRebuildIdx() {
    super("rebuildidx", "[<docType>]", "Rebuild the indexes");
  }

  public void doExecute() throws Throwable {
    final String type = cli.word(null);

    if (type == null) {
      multiLine(jcc.rebuildIndexes());
      return;
    }

    multiLine(jcc.rebuildEntityIndex(type));
  }
}
