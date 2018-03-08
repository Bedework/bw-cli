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

package org.bedework.bwcli.copiedCalFacade;

import org.bedework.access.WhoDefs;

/** Value object to represent a calendar user.
 *
 *   @author Mike Douglass
 *  @version 1.0
 */
public class BwUser extends BwPrincipal {
  /* Temp to avoid schema change */
  private boolean instanceOwner;

  /* ====================================================================
   *                   Constructors
   * ==================================================================== */

  /** Create a guest user
   */
  public BwUser() {
    super();
  }

  @Override
  public int getKind() {
    return WhoDefs.whoTypeUser;
  }

  /* ====================================================================
   *                   Copying methods
   * ==================================================================== */

  /** Copy this to val
   *
   * @param val BwUser
   */
  public void copyTo(final BwUser val) {
    super.copyTo(val);
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public Object clone() {
    /* We do not clone the attached subscriptions if present. These need to
       be cloned explicitly or we might set up a clone loop.
    */
    final BwUser u = new BwUser();
    copyTo(u);

    return u;
  }
}
