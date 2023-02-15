/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli;

import org.bedework.bwcli.bwcmd.CmdAdminGroups;
import org.bedework.bwcli.bwcmd.HttpClient;
import org.bedework.bwcli.copiedCalFacade.responses.AdminGroupsResponse;
import org.bedework.bwcli.jmxcmd.CmdRestoreCalData;
import org.bedework.bwcli.jmxcmd.CmdSelfRegAdduser;
import org.bedework.bwcli.jmxcmd.CmdSync;
import org.bedework.bwcli.jmxcmd.CmdSysStats;
import org.bedework.bwcli.jmxcmd.bwengine.CmdSystem;
import org.bedework.bwcli.toolcmd.ToolCmd;
import org.bedework.bwcli.toolcmd.ToolSource;
import org.bedework.bwcli.toolcmd.ToolUser;
import org.bedework.bwlogs.AccessLogs;
import org.bedework.util.args.Args;
import org.bedework.util.jolokia.JolokiaCli;
import org.bedework.util.jolokia.JolokiaClient;

import java.net.URI;

/**
 * User: mike
 * Date: 5/5/15
 * Time: 4:26 PM
 */
public class BwCli extends JolokiaCli {
  private WebClient webClient;
  private final String url;
  private final String id;
  private final String pw;
  
  // Last response
  private AdminGroupsResponse adgrs;
  
  public BwCli(final String url,
               final String jmxUrl,
               final String id,
               final String pw,
               final boolean debug) throws Throwable {
    super(jmxUrl, debug);
    
    this.url = url;
    this.id = id;
    this.pw = pw;

    register(new CmdAdminGroups());
    
    // jmx
    register(new CmdRestoreCalData());
    register(new CmdSysStats());

    register(new CmdSync());

    register(new CmdSelfRegAdduser());

    // jmx - engine

    register(new CmdSystem());

    register(new ToolCmd());
    register(new ToolSource());
    register(new ToolUser());
  }

  public JolokiaClient makeClient(final String uri) {
    return new JolokiaConfigClient(uri, id, pw);
  }

  public WebClient getWebClient() {
    if (webClient == null) {
      webClient = new WebClient(url);
    }
    
    return webClient;
  }

  public HttpClient getCl() {
    return getWebClient().getCl();
  }
  
  public void setAdgrs(final AdminGroupsResponse val) {
    adgrs = val;
  }
  
  public AdminGroupsResponse getAdgrs() {
    return adgrs;
  }

  /**
   * <p>Arguments<ul>
   *     <li>url: the url of the jolokia service</li>
   * </ul>
   * </p>
   *
   * @param args program arguments.
   */
  public static void main(final String[] args) {
    String url = null;
    String id = null;
    String pw = null;
    String cmd = null;
    String cmdFile = null;
    String jmxUrl = null;
    boolean debug = false;

    try {
      final Args pargs = new Args(args);

      while (pargs.more()) {
        if (pargs.ifMatch("debug")) {
          debug = true;
          continue;
        }

        if (pargs.ifMatch("access")) {
          new AccessLogs().analyze(pargs.next());
          return;  // Always 1 shot
        }

        if (pargs.ifMatch("url")) {
          url = pargs.next();
          continue;
        }

        if (pargs.ifMatch("jmxUrl")) {
          jmxUrl = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-id")) {
          id = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-pw")) {
          pw = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-cmds")) {
          cmdFile = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-cmd")) {
          cmd = pargs.next();
          continue;
        }

        usage("Illegal argument: " +
                      pargs.current());
        return;
      }

      final BwCli jc = new BwCli(url, jmxUrl, id, pw, debug);
      
      if (cmdFile != null) {
        jc.setSingleCmd("sou \"" + cmd + "\"");
      } else if (cmd != null) {
        jc.setSingleCmd(cmd);
      }

      jc.processCmds();
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Client to interact with the bedework web interfaces
   */
  public static class WebClient {
    private final String url;
    
    private HttpClient cl;

    WebClient(final String url) {
      this.url = url;
    }

    public HttpClient getCl() {
      if (cl != null) {
        return cl;
      }

      try {
        cl = new HttpClient(new URI(url));
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }

      return cl;
    }

  }

  private static void usage(final String msg) {
    if (msg != null) {
      System.err.println();
      System.err.println(msg);
    }

    System.err.println();
    System.err.println("Optional arguments:");
    System.err.println("   url <url>          Url of the jolokia jmx service");
    System.err.println("   -cmds <qstring>    A path to a file of commands");
    System.err.println("   -cmd  <qstring>    A single quoted command to execute");
    System.err.println("   debug              To enable debug traces");
  }
}
