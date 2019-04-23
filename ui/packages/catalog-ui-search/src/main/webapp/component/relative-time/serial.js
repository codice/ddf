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
/* loosely based on ISO 8601 time intervals */
const serialize = time => {
  if (!time || time.unit === undefined || time.last === undefined) {
    return
  }
  const prefix = time.unit === 'm' || time.unit === 'h' ? 'PT' : 'P'
  return `RELATIVE(${prefix + time.last + time.unit.toUpperCase()})`
}
const deserialize = value => {
  if (!value) {
    return
  }

  const match = value.match(/RELATIVE\(Z?([A-Z]*)(\d+\.*\d*)(.)\)/)
  if (!match) {
    return
  }

  let [, prefix, last, unit] = match
  last = parseFloat(last)
  unit = unit.toLowerCase()
  if (prefix === 'P' && unit === 'm') {
    //must capitalize months
    unit = unit.toUpperCase()
  }

  return {
    last,
    unit,
  }
}
export { serialize, deserialize }
