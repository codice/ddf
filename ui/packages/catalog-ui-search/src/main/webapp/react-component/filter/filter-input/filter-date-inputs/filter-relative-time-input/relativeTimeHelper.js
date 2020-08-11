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
export const serialize = ({ last, unit }) => {
  if (unit === undefined || !parseFloat(last)) {
    return
  }
  const prefix = unit === 'm' || unit === 'h' ? 'PT' : 'P'
  return `RELATIVE(${prefix + last + unit.toUpperCase()})`
}

export const deserialize = value => {
  if (typeof value !== 'string') {
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

export const relativeTimeError = ({ last, unit }) => {
  console.log(unit)
  const value = getTimeMinutes(last,unit)
  return value > 100000000000
}

const getTimeMinutes=(time, unit)=>{
  if(unit === 'm'){
    return time
  }
  if(unit === 'h'){
    return time * 60 
  }
  if(unit === 'D'){
    return time * 60 * 24
  } 
  if(unit === 'M'){
    return time * 60 * 24 * 30
  }
  if(unit === 'y'){
    return time * 60 * 24 * 365
  }
}
