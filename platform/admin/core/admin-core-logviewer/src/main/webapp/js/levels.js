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

import color from 'color'

const levels = {
  ERROR: 'red',
  WARN: 'yellow',
  INFO: 'white',
  DEBUG: '#0f0',
  TRACE: 'blue',
  ALL: undefined
}

// log level colors
export default (level) => {
  if (level !== undefined) {
    return color(levels[level]).lighten(0.9).hslString()
  }

  return Object.keys(levels)
}
