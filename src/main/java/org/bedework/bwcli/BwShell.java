/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.jmxcmd.CmdListIdx;
import org.bedework.bwcli.jmxcmd.CmdMakeIdxProd;
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
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
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
                  "Example interactive shell with completion and autosuggestions. " +
                          "Hit @|magenta <TAB>|@ to see available commands.",
                  "Hit @|magenta ALT-S|@ to toggle tailtips.",
                  ""},
          footer = {"", "Press Ctrl-D to exit."},
          subcommands = {
                  MyCommand.class,
                  PicocliCommands.ClearScreen.class,
                  CommandLine.HelpCommand.class,
                  CmdMakeIdxProd.class,
                  CmdListIdx.class})
  static public class CliCommands implements Runnable {
    private PrintWriter out;
    private final Config conf;

    private JolokiaConfigClient client;

    CliCommands(final Config conf) {
      this.conf = conf;
    }

    public void setReader(final LineReader reader){
      out = reader.getTerminal().writer();
    }

    public JolokiaConfigClient getClient() {
      if (client == null) {
        client = new JolokiaConfigClient(conf.jmxUrl, conf.id, conf.pw);
      }

      return client;
    }

    public PrintWriter getOut() {
      return out;
    }

    public void run() {
      out.println(new CommandLine(this).getUsageMessage());
    }
  }

  /**
   * A command with some options to demonstrate completion.
   */
  @Command(name = "cmd", mixinStandardHelpOptions = true, version = "1.0",
          description = {"Command with some options to demonstrate TAB-completion.",
                         " (Note that enum values also get completed.)"},
          subcommands = {Nested.class, CommandLine.HelpCommand.class})
  static class MyCommand implements Runnable {
    @Option(names = {"-v", "--verbose"},
            description = { "Specify multiple -v options to increase verbosity.",
                            "For example, `-v -v -v` or `-vvv`"})
    private boolean[] verbosity = {};

    @ArgGroup(exclusive = false)
    private MyDuration myDuration = new MyDuration();

    static class MyDuration {
      @Option(names = {"-d", "--duration"},
              description = "The duration quantity.",
              required = true)
      private int amount;

      @Option(names = {"-u", "--timeUnit"},
              description = "The duration time unit.",
              required = true)
      private TimeUnit unit;
    }

    @ParentCommand
    CliCommands parent;

    public void run() {
      if (verbosity.length > 0) {
        parent.out.printf("Hi there. You asked for %d %s.%n",
                          myDuration.amount, myDuration.unit);
      } else {
        parent.out.println("hi!");
      }
    }
  }

  @Command(name = "nested", mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
          description = "Hosts more sub-subcommands")
  static class Nested implements Runnable {
    public void run() {
      System.out.println("I'm a nested subcommand. I don't do much, but I have sub-subcommands!");
    }

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Multiplies two numbers.")
    public void multiply(@Option(names = {"-l", "--left"}, required = true) int left,
                         @Option(names = {"-r", "--right"}, required = true) int right) {
      System.out.printf("%d * %d = %d%n", left, right, left * right);
    }

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Adds two numbers.")
    public void add(@Option(names = {"-l", "--left"}, required = true) int left,
                    @Option(names = {"-r", "--right"}, required = true) int right) {
      System.out.printf("%d + %d = %d%n", left, right, left + right);
    }

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Subtracts two numbers.")
    public void subtract(@Option(names = {"-l", "--left"}, required = true) int left,
                         @Option(names = {"-r", "--right"}, required = true) int right) {
      System.out.printf("%d - %d = %d%n", left, right, left - right);
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
          } catch (UserInterruptException e) {
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
