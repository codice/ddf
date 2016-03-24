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

const arrayToObject = function (key, array) {
  var o = {}
  array.forEach(function (element) {
    o[element[key]] = element
  })
  return o
}

// filter logic for log level and regex matching.
// also adds <mark/> tags for filter highlighting
export default (filters, logs) => {
  const level = filters.level || 'ALL'

  const fields = Object.keys(filters).filter(function (field) {
    return field !== 'level' && filters[field] !== ''
  })

  const hasMarks = (row) => {
    return Object.keys(row.marks).length === fields.length
  }

  const getMarks = (entry) => {
    var marks = fields.map((field) => {
      if (entry[field]) {
        var match = entry[field].toLowerCase()
          .match(new RegExp(filters[field], 'i'))
        if (match !== null) {
          return {
            field: field,
            start: match.index,
            end: match[0].length + match.index
          }
        }
      }
    }).filter((m) => { return m !== undefined })

    return {
      entry: entry,
      marks: arrayToObject('field', marks)
    }
  }

  const levelLogs = logs.filter((entry) => {
    return level === 'ALL' || entry.level === level
  }).map(getMarks)

  return (fields.length > 0) ? levelLogs.filter(hasMarks) : levelLogs
}
