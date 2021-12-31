/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdSync extends JmxCmd {
  public CmdSync() {
    super("sync", "[get] attrname|cmd [val]",
          "Interact with sync engine. Example commands are\n," +
                  "  sync get HibernateDialect\n" +
                  "  sync HibernateDialect \"val\"\n" +
                  "attrname may be:" +
                  "   HibernateDialect Privkeys, Pubkeys, TimezonesURI\n" +
                  "   SchemaOutFile\n" +
                  "cmd may be:" +
                  "   schema [export], start, stop, resched \"id\"");
  }

  public void doExecute() throws Throwable {
    final String wd = cli.word("schemaWd");

    final boolean get = "get".equals(wd);

    final String attrCmd;

    if (get) {
      attrCmd = cli.word("attrname");
    } else {
      attrCmd = wd;
    }

    if (attrCmd == null) {
      info("Need an attribute or cmd");
      return;
    }

    if ("schema".equals(attrCmd)) {
      multiLine(jcc.syncSchema(cli.nextIs("export"),
                               cli.optionalKeyString("out")));
      return;
    }

    if ("start".equals(attrCmd)) {
      jcc.syncStart();
      return;
    }

    if ("stop".equals(attrCmd)) {
      jcc.syncStop();
      return;
    }

    if ("resched".equals(attrCmd)) {
      info(jcc.syncResched(cli.string("id")));
      return;
    }

    if (get) {
      info(jcc.getSyncAttr(attrCmd));
    } else {
      jcc.setSyncAttr(attrCmd, cli.string(null));
    }
  }
}
