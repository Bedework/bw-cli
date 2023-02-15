/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.bwcli.jmxcmd;

import org.bedework.bwcli.PicoCmd;

import picocli.CommandLine.Option;

/**
 * User: mike
 * Date: 11/11/16
 * Time: 21:47
 */
public abstract class SchemaCmd extends PicoCmd {
  @Option(names = {"export"},
          description = {"If present export result to db"})
  protected boolean export;

  @Option(names = {"out"},
          description = {"If present specify an output file"})
  protected String out;
}
