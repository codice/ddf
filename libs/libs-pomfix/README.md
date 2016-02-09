# pom-fix

Fix all the poms!

## overview

This module will fix common issues with poms related to
dependencies and OSGi.

Issues:

- dependency listed in features.xml but not pom.xml
- dependency listed in plugin section but not dependency section of
  pom.xml

View the `test` directory for examples of bad poms.

## install

To use this tool on the command line, do:

    $ npm ln

This will add a new tool `pom-fix` that can be used on the command line.

## usage

To find any missing dependencies in your poms, do:

    $ pom-fix # exit 0 if no issues found, otherwise exit 1

To run in a specific directory, do:

    $ pom-fix --dir="/path/to/dir" # default dir is pwd

To fix all dependency issues in-place, do:

    $ pom-fix --apply

## test

To run tests, do:

    $ npm test

