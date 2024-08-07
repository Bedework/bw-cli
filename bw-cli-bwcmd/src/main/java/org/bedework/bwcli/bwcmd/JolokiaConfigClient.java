/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.bwcli.bwcmd;

import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jolokia.JolokiaClient;

import java.util.Collections;
import java.util.List;

import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.cmdutilMbean;
import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.dbConfMbean;
import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.dumpRestoreMbean;
import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.eventregMbean;
import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.indexMbean;
import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.selfregMbean;
import static org.bedework.bwcli.bwcmd.copiedCalFacade.Configurations.systemMbean;

/**
 * User: mike Date: 12/3/15 Time: 00:32
 */
public class JolokiaConfigClient extends JolokiaClient {
  private final static String carddavUserDirMbean =
          "org.bedework.carddav:service=CardDav,Type=dirhandler,Name=user-dirHandler";

  private final static String notifierMbean =
          "org.bedework.notify:service=Notify,Type=notifyConf,Name=notifyConf";

  private final static String syncEngineMbean =
          "org.bedework.synch:service=SynchConf";

  private final static String sysMonitorMbean =
          "org.bedework.bwengine:service=BwSysMonitor";

  private final static String tzsvrMbean =
          "org.bedework.timezones:service=TzSvr,Type=tzConf,Name=tzConf";

  // synch connector looks like
  //      org.bedework.synch:service=SynchConf,Type=connector,Name=localBedework
  /**
   *
   * @param url Usually something like "http://localhost:8080/hawtio/jolokia"
   */
  public JolokiaConfigClient(final String url,
                             final String id, 
                             final String pw) {
    super(url, id, pw);
  }

  public String setCmdutilUser(final String account) throws Throwable {
    return execCmdutilCmd("user " + account);
  }

  public String execCmdutilCmd(final String cmd) throws Throwable {
    return execString(cmdutilMbean, "exec", cmd);
  }

  public List<String> coreSchema(final boolean export,
                                 final String out) throws Throwable {
    return doSchema(dbConfMbean, export, out);
  }

  public String listIndexes() throws Throwable {
    return execString(indexMbean, "listIndexes");
  }

  public String purgeIndexes() throws Throwable {
    return execString(indexMbean, "purgeIndexes");
  }

  public String newIndexes() throws Throwable {
    return execString(indexMbean, "newIndexes");
  }

  public List<String> rebuildIndexes() throws Throwable {
    execute(indexMbean, "rebuildIndex");

    String status;
    do {
      status = waitCompletion(indexMbean);
      multiLine(execStringList(indexMbean, "rebuildStatus"));
    } while (status.equals(ConfBase.statusTimedout));

    return execStringList(indexMbean, "rebuildStatus");
  }

  public List<String> sysStats() throws Throwable {
    return execStringList(sysMonitorMbean, "showValues");
  }

  public List<String> rebuildEntityIndex(final String docType) throws Throwable {
    String status =
            execString(indexMbean, "rebuildEntityIndex", docType);

    if (!"Started".equals(status)) {
      return Collections.singletonList("Rebuild start failed: status was " + status);
    }

    do {
      status = waitCompletion(indexMbean);
      multiLine(execStringList(indexMbean, "rebuildStatus"));
    } while (status.equals(ConfBase.statusTimedout));

    return execStringList(indexMbean, "rebuildStatus");
  }

  public Object indexStats(final String indexName) throws Throwable {
    return exec(indexMbean, "indexStats", indexName);
  }

  public String reindex(final String indexName) throws Throwable {
    return execString(indexMbean, "reindex", indexName);
  }

  public List<String> rebuildIdxStatus() throws Throwable {
    return execStringList(indexMbean, "rebuildStatus");
  }

  public String makeIdxProd(final String indexName) throws Throwable {
    return execString(indexMbean, "setProdAlias", indexName);
  }

  public String makeAllIdxProd() throws Throwable {
    return execString(indexMbean, "makeAllProd");
  }

  public List<String> restoreCalData(final String path) throws Throwable {
    if (path != null) {
      writeVal(dumpRestoreMbean, "DataIn", path);
    }

    writeVal(dumpRestoreMbean, "AllowRestore", "true");

    execute(dumpRestoreMbean, "restoreData");

    waitCompletion(dumpRestoreMbean);

    return execStringList(dumpRestoreMbean, "restoreStatus");
  }
  
  /* System properties */

  public void setSystemTzid(final String val) throws Throwable {
    writeVal(systemMbean, "Tzid", val);
  }
  
  public String getSystemTzid() throws Throwable {
    return readString(systemMbean, "Tzid");
  }

  public void setRootUsers(final String val) throws Throwable {
    writeVal(systemMbean, "RootUsers", val);
  }

  public String getRootUsers() throws Throwable {
    return readString(systemMbean, "RootUsers");
  }

  public void setAutoKillMinutes(final Integer val) throws Throwable {
    writeVal(systemMbean, "AutoKillMinutes", val);
  }

  public Integer getAutoKillMinutes() throws Throwable {
    final String s = readString(systemMbean, "AutoKillMinutes");
    return Integer.valueOf(s);
  }

  /* ----------- sync engine ----------------- */

  public String getSyncAttr(final String attrName) throws Throwable {
    return readString(syncEngineMbean, attrName);
  }

  public void setSyncAttr(final String attrName,
                          final String val) throws Throwable {
    writeVal(syncEngineMbean, attrName, val);
  }

  public List<String> syncSchema(final boolean export,
                                 final String out) throws Throwable {
    return doSchema(syncEngineMbean, export, out);
  }

  public void syncStart() throws Throwable {
    execute(syncEngineMbean, "start");
  }

  public void syncStop() throws Throwable {
    execute(syncEngineMbean, "stop");
  }

  public String syncResched(final String id) throws Throwable {
    return execString(syncEngineMbean, "rescheduleNow", id);
  }

  public void setSyncPrivKeys(final String val) throws Throwable {
    writeVal(syncEngineMbean, "PrivKeys", val);
  }

  /* ----------- carddav ----------------- */

  public List<String> carddavSchema(final boolean export,
                                    final String out) throws Throwable {
    return doSchema(carddavUserDirMbean, export, out);
  }

  /* ----------- notifier ----------------- */

  public List<String> notifierSchema(final boolean export,
                                     final String out) throws Throwable {
    return doSchema(notifierMbean, export, out);
  }

  /* ----------- selfreg ----------------- */

  public String selfregAddUser(final String account,
                               final String first,
                               final String last,
                               final String email,
                               final String pw) throws Throwable {
    return execString(selfregMbean, "addUser",
                      account, first, last, email, pw);
  }

  public List<String> selfregSchema(final boolean export,
                                    final String out) throws Throwable {
    return doSchema(selfregMbean, export, out);
  }

  /* ----------- eventreg ----------------- */

  public List<String> eventregSchema(final boolean export,
                                     final String out) throws Throwable {
    return doSchema(eventregMbean, export, out);
  }

  /* ----------- generic ----------------- */

  public void setAttr(final String mbean,
                      final String attrName,
                      final String val) throws Throwable {
    writeVal(mbean, attrName, val);
  }

  public List<String> doSchema(final String mbean,
                               final boolean export,
                               final String out) throws Throwable {
    writeVal(mbean, "Export", String.valueOf(export));

    if (out != null) {
      writeVal(mbean, "SchemaOutFile", out);
    }

    execute(mbean, "schema");

    waitCompletion(mbean);

    return execStringList(mbean, "schemaStatus");
  }

  /* ----------- timezone server ----------------- */

  public String getTzAttr(final String attrName) throws Throwable {
    return readString(tzsvrMbean, attrName);
  }

  public void setTzAttr(final String attrName,
                        final String val) throws Throwable {
    writeVal(tzsvrMbean, attrName, val);
  }

  public String tzRefreshData() throws Throwable {
    return execString(tzsvrMbean, "refreshData");
  }
}
