# bw-cli [![Build Status](https://travis-ci.org/Bedework/bw-cli.svg)](https://travis-ci.org/Bedework/bw-cli)

This project provides a command line client for
[Bedework](https://www.apereo.org/projects/bedework).

## Requirements

1. JDK 11
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release:

> mvn -P bedework-dev release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn -P bedework-dev release:perform

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.9
    * Bump jackson version
    * Refactor: move cli support out of bw-util into bw-cliutil
    * Add log analysis code
    * Improve readability
    * Acces log analyzer
    * Fixes to parsing and output
    * Move access log stuff into bw-logs
    
    
