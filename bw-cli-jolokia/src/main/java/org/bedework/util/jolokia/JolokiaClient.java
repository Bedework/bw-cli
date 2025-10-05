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
package org.bedework.util.jolokia;

import org.bedework.base.response.GetEntitiesResponse;
import org.bedework.base.response.GetEntityResponse;
import org.bedework.base.response.Response;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import org.jolokia.client.BasicAuthenticator;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pWriteRequest;

import java.util.Collection;

/**
 * User: mike Date: 12/3/15 Time: 00:32
 */
public class JolokiaClient implements Logged {
  private final String url;
  private final String id;
  private final String pw;
  private J4pClient client;

  /**
   *
   * @param url Usually something like "http://localhost:8080/hawtio/jolokia"
   */
  public JolokiaClient(final String url) {
    this.url = url;
    this.id = null;
    this.pw = null;
  }

  /**
   *
   * @param url Usually something like "http://localhost:8080/hawtio/jolokia"
   */
  public JolokiaClient(final String url,
                       final String id,
                       final String pw) {
    this.url = url;
    this.id = id;
    this.pw = pw;
  }

  public J4pClient getClient() {
    if (client != null) {
      return client;
    }

    if (id == null) {
      client = J4pClient.url(url).build();

      return client;
    }

    client = J4pClient.url(url)
                      .user(id)
                      .password(pw)
                      .authenticator(new BasicAuthenticator().preemptive())
                      .connectionTimeout(3000)
                      .build();

    return client;
  }

  public Response<?> writeVal(final String objectName,
                              final String name,
                              final Object val) {
    final var resp = new Response<>();
    try {
      final var request =
              new J4pWriteRequest(objectName, name, val);
      request.setPreferredHttpMethod("POST");
      getClient().execute(request);
      return resp;
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  public GetEntityResponse<String> readString(
          final String objectName,
          final String name) {
    final var resp = new GetEntityResponse<String>();
    try {
      final var request =
              new J4pReadRequest(objectName, name);
      request.setPreferredHttpMethod("POST");
      final var response = getClient().execute(request);
      return resp.setEntity(response.getValue());
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  /**
   *
   * @param objectName of mbean
   * @param operation that returns a list
   * @return the list
   */
  public GetEntitiesResponse<String> execStringList(
          final String objectName,
          final String operation) {
    final var resp = new GetEntitiesResponse<String>();
    try {
      final var execRequest =
              new J4pExecRequest(objectName, operation);
      execRequest.setPreferredHttpMethod("POST");
      final var response = getClient().execute(execRequest);
      return resp.setEntities(response.getValue());
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  /**
   *
   * @param objectName of mbean
   * @param operation that returns a string
   * @return response holding the string
   */
  public GetEntityResponse<String> execString(
          final String objectName,
          final String operation,
          final Object... args) {
    final var resp = new GetEntityResponse<String>();
    try {
      final var execRequest =
              new J4pExecRequest(objectName, operation, args);
      execRequest.setPreferredHttpMethod("POST");
      final J4pResponse<J4pExecRequest> response =
              getClient().execute(execRequest);
      return resp.setEntity(response.getValue());
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  /**
   *
   * @param objectName of mbean
   * @param operation that returns a string
   * @return the object
   */
  public GetEntityResponse<Object> exec(
          final String objectName,
          final String operation,
          final Object... args) {
    final var resp = new GetEntityResponse<>();
    try {
      final var execRequest =
              new J4pExecRequest(objectName, operation, args);
      execRequest.setPreferredHttpMethod("POST");
      final var response = getClient().execute(execRequest);
      return resp.setEntity(response.getValue());
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  public Response<?> execute(final String objectName,
                             final String operation,
                             final Object... args) {
    final var resp = new Response<>();
    try {
      final var execRequest =
              new J4pExecRequest(objectName, operation, args);
      getClient().execute(execRequest);
      return resp;
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  /**
   *
   * @param objectName of mbean that has a String Status attribute
   * @return the current status
   */
  public GetEntityResponse<String> getStatus(final String objectName) {
    return readString(objectName, "Status");
  }

  public GetEntityResponse<String> getMemory() {
    return execString("java.lang:type=Memory", "HeapMemoryUsage");
  }

  /**
   *
   * @param objectName of mbean
   * @return String ending status - "Done" or success
   */
  public GetEntityResponse<String> waitCompletion(final String objectName) {
    return waitCompletion(objectName, 60, 10);
  }

  /**
   *
   * @param objectName of mbean
   * @param waitSeconds how long we wait in total
   * @param pollSeconds poll interval
   * @return String ending status - "Done" or success
   */
  public GetEntityResponse<String> waitCompletion(
          final String objectName,
          final long waitSeconds,
          final long pollSeconds) {
    /* The process will start off in stopped state.
       If we see it stopped, it's because it hasn't got going yet.
     */
    final var resp = new GetEntityResponse<String>();

    try {
      final long start = System.currentTimeMillis();
      double curSecs;
      final long pollWait = pollSeconds * 1000;

      boolean starting = true;

      do {
        final var statusResp = getStatus(objectName);

        if (!statusResp.isOk()) {
          return resp.fromResponse(statusResp);
        }

        final var status = statusResp.getEntity();

        if (starting && status.equals(ConfBase.statusStopped)) {
          info("Waiting for process to start");
        } else {
          starting = false;

          if (status.equals(ConfBase.statusDone)) {
            info("Received status Done");
            return resp;
          }

          if (!status.equals(ConfBase.statusRunning)) {
            error("Status is " + status);
            return resp;
          }
        }

        info("Still running...");

        final long now = System.currentTimeMillis();
        curSecs = (double)(now - start) / 1000;

        synchronized (this) {
          this.wait(pollWait);
        }
      } while (curSecs < waitSeconds);

      // Treat timedout as OK
      return resp.setEntity(ConfBase.statusTimedout);
    } catch (final Throwable t) {
      error(t);
      return resp.setStatus(Response.Status.failed)
                 .setEntity(ConfBase.statusFailed);
    }
  }

  protected void multiLine(final Collection<String> resp) {
    if (resp == null) {
      info("Null response");
      return;
    }

    for (final String s: resp) {
      info(s);
    }
  }

  /* ======================================================
   *                   Logged methods
   * ====================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
