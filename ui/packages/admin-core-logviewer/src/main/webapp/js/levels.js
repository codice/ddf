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

// Correspond to classes in log-entry.less
// Also used to populate level-selector drop down list
// these should be ordered by severity from lowest to highest
const levels = {
  ALL: undefined,
  TRACE: 'traceLevel',
  DEBUG: 'debugLevel',
  INFO: 'infoLevel',
  WARN: 'warnLevel',
  ERROR: 'errorLevel',
}

// log level colors
export default level => {
  if (level !== undefined) {
    return levels[level]
  }

  return Object.keys(levels)
}
