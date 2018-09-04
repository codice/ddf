/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import random from 'random-item'

import levels from '../../main/webapp/js/levels'

const bundleName = [
  'catalog.bundle',
  'platform.bundle',
  'security.bundle',
  'utils.bundle',
]

const bundleVersion = ['1.2.3', '3.4.5', '5.6.7']

const messages = [
  'First log message',
  'Second log message',
  'Third log message',
]

export default function(o) {
  return {
    timestamp: new Date().toISOString(),
    level: random(levels().slice(1)),
    bundleName: random(bundleName),
    bundleVersion: random(bundleVersion),
    message: random(messages),
    ...o,
  }
}
