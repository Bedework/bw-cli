/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd;

import org.bedework.bwcli.bwcmd.copiedCalFacade.BwAdminGroup;
import org.bedework.bwcli.bwcmd.copiedCalFacade.BwGroup;
import org.bedework.bwcli.bwcmd.copiedCalFacade.responses.AdminGroupsResponse;

import picocli.CommandLine;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
@CommandLine.Command(name = "adgs",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = { CommandLine.HelpCommand.class },
        description = {
                "List the admin groups."})
public class CmdAdminGroups extends PicoCmd {
  public void doExecute() {
    final AdminGroupsResponse adgrs;
    try {
      adgrs = getCl().getJson("/feeder/admingroups/json.gdo",
                              AdminGroupsResponse.class);
    } catch (final Exception e) {
      info(e);
      return;
    }

    if (adgrs == null) {
      info("No response");
      return;
    }
    
    int i = 1;
    
    for (final BwGroup gr: adgrs.getGroups()) {
      final BwAdminGroup adgr = (BwAdminGroup)gr;
      
      info(i + ": " + adgr.getAccount() + " " + adgr.getDescription());
      i++;
    }
  }
}
