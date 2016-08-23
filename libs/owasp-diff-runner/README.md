<!--
/*
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
-->

Description:
    -OWASP diff runner executes OWASP on any changed poms that reside in the current branch commits. This is intended to be used for pull requests but can be used locally as well.

How to use:
    - Activate the OWASP diff runner by specifying the `-Powasp-diff-runner` flag from the owasp-diff-runner module or any parent module.

Important info:
    - Compares ONLY commits, if you have changes that aren't committed then OWASP will not run against those changes.
    - OWASP diff runner runs against the local master branch and assumes the name of the branch is `master`. This also means if the local master branch or your current branch is not up to date with the latest master, incorrect pom differences may be run.
    - OWASP diff runner runs against the ENTIRE pom if changed, regardless if a dependency was added.
    - OWASP diff runner will only fail on the CVS level defined in dependency-check-maven plugin of the DDF root pom.

