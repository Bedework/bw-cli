/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.toolcmd;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:48
 */
@Command(name = "toolsou",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "Execute the commands in a file."})
public class CmdToolSource extends PicoCmd {
  @Parameters(index = "0",
          paramLabel = "<path>",
          description = {"path to file"}, arity = "1")
  private String path;

  public void doExecute() throws Throwable {
    try {
      final InputStream is = new FileInputStream(path.trim());

      final LineNumberReader lis =
              new LineNumberReader(new InputStreamReader(is));

      for (;;) {
        final String ln = lis.readLine();

        if (ln == null) {
          break;
        }

        info(ln);

        if (ln.startsWith("#")) {
          continue;
        }

        info(client().execCmdutilCmd(ln.trim()));
      }
    } catch (final Throwable t) {
      t.printStackTrace();
      info(t.getLocalizedMessage());
    }
  }
}
