# bw-cli [![Build Status](https://travis-ci.org/Bedework/bw-cli.svg)](https://travis-ci.org/Bedework/bw-cli)

This project provides a command line client for
[Bedework](https://www.apereo.org/projects/bedework).

## Requirements

1. JDK 21
2. Maven 3

## Using this project
See documentation at [github pages for this project](https://bedework.github.io/bw-cli/)

## Reporting Issues
Please report issues via the github issues tab at
> https://github.com/Bedework/bw-cli/issues

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security - Vulnerability reporting
See [SECURITY.md](SECURITY.md).

## Release Notes
### 4.0.9
* Bump jackson version
* Refactor: move cli support out of bw-util into bw-cliutil
* Add log analysis code
* Improve readability
* Access log analyzer
* Fixes to parsing and output
* Move access log stuff into bw-logs
    
### 5.0.0
* Use bedework-parent for builds.
* Don't return a result from Process - not using it.
* Was counting ip addresses on req in and out.
* Added postgres support
* Add cardschema command
* Add cardschema command

### 5.0.1
* Update library versions
* Factor out some code to allow other uses.
* Add classes to allow display of sessions
* Try to make display more compact
* Add a place holder for missing request in.
* Better handling of multi-line info and provision of a summary mode
* Add display modes. Filter by date/time. Allow skipping of totals.
* It was the taskId not sessionId
* Improvements to display
* Allow filter by session id
* Reorder output
* Add demo jline shell to try it as replacement
* Add history file. Add a couple of real commands and add log4j to the runnable target
* Remove log processing code now in bw-log
* Almost complete move to picocli
* Make schema commands subcommands
* Mostly completed move to picocli
* Mostly completed move to picocli. Removed more unused classes and dependency
* Avoid adding extra line endings

### 5.0.2
* Update library versions
* Refactor bw-cli into a multi-module project. Update library versions.

### 5.0.3
* Update library versions
* Make AccessException subclass of RuntimeException.
* ToString changes

### 5.0.4
* Update library versions
* Add module for db interactions
* Move response classes and ToString into bw-base module.
* Pre-jakarta
