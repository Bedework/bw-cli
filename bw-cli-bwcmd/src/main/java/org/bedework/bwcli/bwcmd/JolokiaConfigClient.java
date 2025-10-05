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

import org.bedework.base.response.GetEntitiesResponse;
import org.bedework.base.response.GetEntityResponse;
import org.bedework.base.response.Response;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jolokia.JolokiaClient;

import java.util.Collections;

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

  public GetEntityResponse<String> setCmdutilUser(final String account)  {
    return execCmdutilCmd("user " + account);
  }

  public GetEntityResponse<String> execCmdutilCmd(final String cmd) {
    return execString(cmdutilMbean, "exec", cmd);
  }

  public GetEntitiesResponse<String> coreSchema(final boolean export,
                                                final String out) {
    return doSchema(dbConfMbean, export, out);
  }

  public GetEntityResponse<String> listIndexes() {
    return execString(indexMbean, "listIndexes");
  }

  public GetEntityResponse<String> purgeIndexes() {
    return execString(indexMbean, "purgeIndexes");
  }

  public GetEntityResponse<String> newIndexes() {
    return execString(indexMbean, "newIndexes");
  }

  public GetEntitiesResponse<String> rebuildIndexes()  {
    final var resp = new GetEntitiesResponse<String>();

    final var eresp =
            execute(indexMbean, "rebuildIndex");
    if (!eresp.isOk()) {
      return resp.fromResponse(eresp);
    }

    return waitRebuildCompletion();
  }

  public GetEntitiesResponse<String> sysStats()  {
    return execStringList(sysMonitorMbean, "showValues");
  }

  public GetEntitiesResponse<String> rebuildEntityIndex(final String docType)  {
    final var resp = new GetEntitiesResponse<String>();

    final var eresp =
            execString(indexMbean, "rebuildEntityIndex", docType);
    if (!eresp.isOk()) {
      return resp.fromResponse(eresp);
    }

    final var status = eresp.getEntity();
    if (!"Started".equals(status)) {
      return resp.setEntities(Collections.singletonList(
              "Rebuild start failed: status was " + status));
    }

    return waitRebuildCompletion();
  }

  public GetEntitiesResponse<String> waitRebuildCompletion() {
    final var resp = new GetEntitiesResponse<String>();

    GetEntityResponse<String> wc;
    do {
      wc = waitCompletion(indexMbean);
      if (!wc.isOk()) {
        return resp.fromResponse(wc);
      }

      final var esl = execStringList(indexMbean, "rebuildStatus");
      if (!esl.isOk()) {
        return resp.fromResponse(esl);
      }
      multiLine(esl.getEntities());
    } while (wc.getEntity().equals(ConfBase.statusTimedout));

    return execStringList(indexMbean, "rebuildStatus");
  }

  public Object indexStats(final String indexName)  {
    return exec(indexMbean, "indexStats", indexName);
  }

  public GetEntityResponse<String> reindex(final String indexName)  {
    return execString(indexMbean, "reindex", indexName);
  }

  public GetEntitiesResponse<String> rebuildIdxStatus()  {
    return execStringList(indexMbean, "rebuildStatus");
  }

  public GetEntityResponse<String> makeIdxProd(final String indexName)  {
    return execString(indexMbean, "setProdAlias", indexName);
  }

  public GetEntityResponse<String> makeAllIdxProd()  {
    return execString(indexMbean, "makeAllProd");
  }

  public GetEntitiesResponse<String> dumpCalData(final String path)  {
    if (path != null) {
      writeVal(dumpRestoreMbean, "DataOut", path);
    }

    writeVal(dumpRestoreMbean, "NewDumpFormat", "true");

    execute(dumpRestoreMbean, "dumpData");

    waitCompletion(dumpRestoreMbean);

    return execStringList(dumpRestoreMbean, "dumpStatus");
  }

  public GetEntitiesResponse<String> restoreCalData(final String path)  {
    if (path != null) {
      writeVal(dumpRestoreMbean, "DataIn", path);
    }

    writeVal(dumpRestoreMbean, "AllowRestore", "true");

    execute(dumpRestoreMbean, "restoreData");

    waitCompletion(dumpRestoreMbean);

    return execStringList(dumpRestoreMbean, "restoreStatus");
  }

  /* System properties */

  public Response<?> setSystemTzid(final String val)  {
    return writeVal(systemMbean, "Tzid", val);
  }

  public GetEntityResponse<String> getSystemTzid()  {
    return readString(systemMbean, "Tzid");
  }

  public Response<?> setRootUsers(final String val)  {
    return writeVal(systemMbean, "RootUsers", val);
  }

  public GetEntityResponse<String> getRootUsers()  {
    return readString(systemMbean, "RootUsers");
  }

  public Response<?> setAutoKillMinutes(final Integer val)  {
    return writeVal(systemMbean, "AutoKillMinutes", val);
  }

  public GetEntityResponse<Integer> getAutoKillMinutes()  {
    final var resp = new GetEntityResponse<Integer>();
    final var r = readString(systemMbean, "AutoKillMinutes");

    if (!r.isOk()) {
      return resp.fromResponse(r);
    }
    return resp.setEntity(Integer.valueOf(r.getEntity()));
  }

  /* ----------- sync engine ----------------- */

  public GetEntityResponse<String> getSyncAttr(final String attrName)  {
    return readString(syncEngineMbean, attrName);
  }

  public Response<?> setSyncAttr(final String attrName,
                                 final String val)  {
    return writeVal(syncEngineMbean, attrName, val);
  }

  public GetEntitiesResponse<String> syncSchema(final boolean export,
                                                final String out)  {
    return doSchema(syncEngineMbean, export, out);
  }

  public Response<?> syncStart()  {
    return execute(syncEngineMbean, "start");
  }

  public Response<?> syncStop()  {
    return execute(syncEngineMbean, "stop");
  }

  public GetEntityResponse<String> syncResched(final String id)  {
    return execString(syncEngineMbean, "rescheduleNow", id);
  }

  public Response<?> setSyncPrivKeys(final String val)  {
    return writeVal(syncEngineMbean, "PrivKeys", val);
  }

  /* ----------- carddav ----------------- */

  public GetEntitiesResponse<String> carddavSchema(final boolean export,
                                                   final String out)  {
    return doSchema(carddavUserDirMbean, export, out);
  }

  /* ----------- notifier ----------------- */

  public GetEntitiesResponse<String> notifierSchema(final boolean export,
                                                    final String out)  {
    return doSchema(notifierMbean, export, out);
  }

  /* ----------- selfreg ----------------- */

  public GetEntityResponse<String> selfregAddUser(final String account,
                                                  final String first,
                                                  final String last,
                                                  final String email,
                                                  final String pw)  {
    return execString(selfregMbean, "addUser",
                      account, first, last, email, pw);
  }

  public GetEntitiesResponse<String> selfregSchema(final boolean export,
                                                   final String out)  {
    return doSchema(selfregMbean, export, out);
  }

  /* ----------- eventreg ----------------- */

  public GetEntitiesResponse<String> eventregSchema(final boolean export,
                                                    final String out)  {
    return doSchema(eventregMbean, export, out);
  }

  /* ----------- generic ----------------- */

  public Response<?> setAttr(final String mbean,
                             final String attrName,
                             final String val)  {
    return writeVal(mbean, attrName, val);
  }

  public GetEntitiesResponse<String> doSchema(final String mbean,
                                              final boolean export,
                                              final String out)  {
    final var resp = new GetEntitiesResponse<String>();
    var wresp = writeVal(mbean, "Export", String.valueOf(export));

    if (!wresp.isOk()) {
      return resp.fromResponse(wresp);
    }

    if (out != null) {
      wresp = writeVal(mbean, "SchemaOutFile", out);
      if (!wresp.isOk()) {
        return resp.fromResponse(wresp);
      }
    }

    final var eresp = execute(mbean, "schema");
    if (!eresp.isOk()) {
      return resp.fromResponse(eresp);
    }

    final var wcresp = waitCompletion(mbean);
    if (!wcresp.isOk()) {
      return resp.fromResponse(wcresp);
    }

    return execStringList(mbean, "schemaStatus");
  }

  /* ----------- timezone server ----------------- */

  public GetEntityResponse<String> getTzAttr(final String attrName)  {
    return readString(tzsvrMbean, attrName);
  }

  public Response<?> setTzAttr(final String attrName,
                               final String val)  {
    return writeVal(tzsvrMbean, attrName, val);
  }

  public GetEntityResponse<String> tzRefreshData()  {
    return execString(tzsvrMbean, "refreshData");
  }
}
