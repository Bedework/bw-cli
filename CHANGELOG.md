# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.1.0-SNAPSHOT)

## [6.0.0] - 2025-06-25
* First jakarta release
* Move bw-util-jolokia/JolokiaClient into this project as bw-cli-jolokia. bw-cliutil no longer needed.

## [5.0.4] - 2025-02-06
* Update library versions
* Add module for db interactions
* Move response classes and ToString into bw-base module.
* Pre-jakarta

## [5.0.3] - 2024-10-22
* Update library versions
* Make AccessException subclass of RuntimeException.
* ToString changes

## [5.0.2] - 2024-07-26
* Update library versions
* Refactor bw-cli into a multi-module project. Update library versions.

## [5.0.1] - 2024-03-22
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

## [5.0.0] - 2022-02-12
* Use bedework-parent for builds.
* Don't return a result from Process - not using it.
* Was counting ip addresses on req in and out.
* Added postgres support
* Add cardschema command
* Add cardschema command

## [4.0.9] - 2020-03-22
* More log info
* Add access log analyzer to count ips
* Bump jackson version
* Refactor: move cli support out of bw-util into bw-cliutil
* Add log analysis code
* Improve readability
* Access log analyzer
* Fixes to parsing and output
* Move access log stuff into bw-logs


## [4.0.8] - 2019-10-16
* Update library versions.

## [4.0.7] - 2019-08-27
* Try different goal for building package

## [4.0.6] - 2019-08-27
* Update library versions.

## [4.0.5] - 2019-06-27
* Update library versions.
* More log processing checks. Change start year to 1600

## [4.0.4] - 2019-04-15
* Update library versions.
* Analyze acccess logs
* Add list of ips responsible for long requests

## [4.0.3] - 2019-01-07
* Update library versions.
* Add feature to process log files

## [4.0.2] - 2018-12-4
* Create tzrefresh command

## [4.0.1] - 2018-11-27
* Add command to get system monitor figures.
* Implement (re)index of single entity type - let's me add resources to the index.
* Add rescheduleNow command
* Add single command option - and fix cmds

## [4.0.0] - 2018-04-08
* Fixes to self reg create account. Add some cli commands to build schema and create accounts.

