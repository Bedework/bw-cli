/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.bwcmd.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@Command(name = "tz",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Timezone service commands."})
public class CmdTz extends PicoCmd {
  @Command(name = "getattr",
          mixinStandardHelpOptions = true, version = "1.0",
          subcommands = { CommandLine.HelpCommand.class },
          description = {
                  "Get timezone server attributes"})
  void getAttr(@Parameters(arity = "1..*",
          paramLabel = "<attributeNames>",
          description = "attributes to fetch")
               final String[] attrnames) {
    for (final String attrname: attrnames) {
      try {
        info(client().getTzAttr(attrname));
      } catch (final Throwable t) {
        t.printStackTrace(getOut());
      }
    }
  }

  @Command(name = "refresh",
          mixinStandardHelpOptions = true, version = "1.0",
          subcommands = { CommandLine.HelpCommand.class },
          description = {
                  "Refresh the timezone data"})
  void refresh() {
      try {
        info(client().tzRefreshData());
      } catch (final Throwable t) {
        t.printStackTrace(getOut());
    }
  }

  @Command(name = "setattr",
          mixinStandardHelpOptions = true, version = "1.0",
          subcommands = { CommandLine.HelpCommand.class },
          description = {
                  "Set a timezone server attribute"})
  void setAttr(@Parameters(arity = "1",
          paramLabel = "<attributeName>",
          description = "attribute to set")
               final String attrname,
               @Parameters(arity = "1",
                       paramLabel = "<attributeValue>",
                       description = "attribute value")
               final String attrval) {
    try {
      client().setTzAttr(attrname, attrval);
    } catch (final Throwable t) {
        t.printStackTrace(getOut());
    }
  }
}
