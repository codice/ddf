/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
'use strict'

// Based on similar script in Facebook/React
// https://github.com/facebook/react/blob/e8e62ebb595434db40ee9a079189b1ee7e111ffe/scripts/shared/listChangedFiles.js
const path = require('path')
const execFileSync = require('child_process').execFileSync

const exec = (command, args) => {
  console.log('> ' + [command].concat(args).join(' '))
  const options = {
    cwd: process.cwd(),
    env: process.env,
    stdio: 'pipe',
    encoding: 'utf-8',
  }
  return execFileSync(command, args, options)
}

const execGitCmd = args =>
  exec('git', args)
    .trim()
    .toString()
    .split('\n')

const listChangedFiles = () => {
  const rootDir = execGitCmd(['rev-parse', '--show-toplevel'])[0]
  const mergeBase = execGitCmd(['merge-base', 'HEAD', 'origin/master'])
  return [
    ...execGitCmd(['diff', '--name-only', '--diff-filter=ACMRTUB', mergeBase]),
    ...execGitCmd([
      'ls-files',
      '--full-name',
      '--others',
      '--modified',
      '--exclude-standard',
    ]),
  ].map(file => path.resolve(path.join(rootDir, file)))
}

module.exports = listChangedFiles
