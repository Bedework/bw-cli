/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.bwcmd;

import org.bedework.bwcli.copiedCalFacade.BwAdminGroup;
import org.bedework.bwcli.copiedCalFacade.BwGroup;
import org.bedework.bwcli.copiedCalFacade.responses.AdminGroupsResponse;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:45
 */
public class CmdAdminGroups extends ApiCommand {
  public CmdAdminGroups() {
    super("adgs", null, "List the admin groups");
  }

  public void doExecute() throws Throwable {
    final AdminGroupsResponse adgrs =
            getCl().getJson("/feeder/admingroups/json.gdo",
                            AdminGroupsResponse.class);
    
    getBwCli().setAdgrs(adgrs);

    if (adgrs == null) {
      info("No response");
      return;
    }
    
    int i = 1;
    
    for (final BwGroup gr: adgrs.getGroups()) {
      final BwAdminGroup adgr = (BwAdminGroup)gr;
      
      info(String.valueOf(i) + ": " + adgr.getAccount() + " " + adgr.getDescription());
      i++;
    }
  }
}
