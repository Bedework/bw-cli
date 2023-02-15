/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.jmxcmd.CmdJmxSetAttr;
import org.bedework.bwcli.jmxcmd.CmdTz;
import org.bedework.bwcli.jmxcmd.index.CmdIdx;
import org.bedework.bwcli.jmxcmd.schema.CmdSchema;
import org.bedework.util.args.Args;

import org.apache.commons.lang.SystemUtils;
import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * User: mike Date: 1/29/23 Time: 17:43
 */
public class BwShell {
  /**
   * Top-level command that just prints help.
   */
  @Command(name = "",
          description = {
                  "Interactive shell with completion and autosuggestions. " +
                          "Hit @|magenta <TAB>|@ to see available commands.",
                  "Hit @|magenta ALT-S|@ to toggle tailtips.",
                  ""},
          footer = {"", "Press Ctrl-D to exit."},
          subcommands = {
                  PicocliCommands.ClearScreen.class,
                  CommandLine.HelpCommand.class,
                  CmdIdx.class,
                  CmdJmxSetAttr.class,
                  CmdSchema.class,
                  CmdTz.class,
  })
  static public class CliCommands implements PicoCmdI {
    private PrintWriter out;
    private final Config conf;

    private JolokiaConfigClient client;

    CliCommands(final Config conf) {
      this.conf = conf;
    }

    public void setReader(final LineReader reader){
      out = reader.getTerminal().writer();
    }

    public JolokiaConfigClient client() {
      if (client == null) {
        client = new JolokiaConfigClient(conf.jmxUrl, conf.id, conf.pw);
      }

      return client;
    }

    public PrintWriter getOut() {
      return out;
    }

    public void doExecute() {
      out.println(new CommandLine(this).getUsageMessage());
    }
  }

  static class Config {
    String jmxUrl;
    String id;
    String pw;
  }

  public static void main(final String[] args) {
    final Config conf = new Config();

    AnsiConsole.systemInstall();
    History history = null;

    try {
      final Args pargs = new Args(args);

      while (pargs.more()) {
        if (pargs.ifMatch("jmxUrl")) {
          conf.jmxUrl = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-id")) {
          conf.id = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-pw")) {
          conf.pw = pargs.next();
          continue;
        }
      }

      final Supplier<Path> workDir =
              () -> Paths.get(System.getProperty("user.dir"));
      // set up JLine built-in commands
      final Builtins builtins = new Builtins(workDir,
                                             new ConfigurationPath(null, null), null);
      builtins.rename(Builtins.Command.TTOP, "top");
      builtins.alias("zle", "widget");
      builtins.alias("bindkey", "keymap");
      // set up picocli commands
      final CliCommands commands = new CliCommands(conf);

      final PicocliCommandsFactory factory =
              new PicocliCommandsFactory();
      // Or, if you have your own factory, you can chain them like this:
      // MyCustomFactory customFactory = createCustomFactory(); // your application custom factory
      // PicocliCommandsFactory factory = new PicocliCommandsFactory(customFactory); // chain the factories

      final CommandLine cmd = new CommandLine(commands, factory);
      final PicocliCommands picocliCommands = new PicocliCommands(cmd);

      final Parser parser = new DefaultParser();

      try (final Terminal terminal = TerminalBuilder.builder().build()) {
        final SystemRegistry systemRegistry =
                new SystemRegistryImpl(parser,
                                       terminal,
                                       workDir,
                                       null);
        systemRegistry.setCommandRegistries(
                builtins, picocliCommands);
        systemRegistry.register("help", picocliCommands);

        final Path userHomePath =
                SystemUtils.getUserHome().toPath();
        final Path historyPath =
                Paths.get(userHomePath.toString(),
                          ".bwshell_history");
        final LineReader reader = LineReaderBuilder
                .builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser)
                .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                .variable(LineReader.HISTORY_FILE, historyPath)
                .build();
        history = reader.getHistory();
        builtins.setLineReader(reader);
        commands.setReader(reader);
        factory.setTerminal(terminal);
        final TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
        widgets.enable();
        final KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
        keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

        final String prompt = "bw> ";
        String rightPrompt = null;

        // start the shell and process input until the user quits with Ctrl-D
        String line;
        while (true) {
          try {
            systemRegistry.cleanUp();
            line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
            systemRegistry.execute(line);
          } catch (final UserInterruptException ignored) {
            // Ignore
          } catch (final EndOfFileException eofe) {
            return;
          } catch (final Exception e) {
            systemRegistry.trace(e);
          }
        }
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    } finally {
      if (history != null) {
        try {
          history.save();
        } catch (final IOException ioe) {
          System.err.println("Unable to write history");
        }
      }
      AnsiConsole.systemUninstall();
    }
  }
}
