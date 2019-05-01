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

var metacardDefinitions = require('../../component/singletons/metacard-definitions.js')

require('backbone-associations')

function parseMultiValue(value) {
  if (value && value.constructor === Array) {
    return value[0]
  }
  return value
}

function isEmpty(value) {
  return value === undefined || value === null
}

function parseValue(value, attribute) {
  var attributeDefinition = metacardDefinitions.metacardTypes[attribute]
  if (!attributeDefinition) {
    return value.toString().toLowerCase()
  }
  switch (attributeDefinition.type) {
    case 'DATE':
    case 'BOOLEAN':
      return value
    case 'STRING':
      return value.toString().toLowerCase()
    default:
      return parseFloat(value)
  }
}

function compareValues(aVal, bVal, sorting) {
  var sortOrder = sorting.direction === 'descending' ? -1 : 1
  aVal = parseValue(aVal, sorting.attribute)
  bVal = parseValue(bVal, sorting.attribute)
  if (aVal < bVal) {
    return sortOrder * -1
  }
  if (aVal > bVal) {
    return sortOrder
  }
  return 0
}

function checkSortValue(a, b, sorting) {
  var aVal = parseMultiValue(a.get('metacard>properties>' + sorting.attribute))
  var bVal = parseMultiValue(b.get('metacard>properties>' + sorting.attribute))
  if (isEmpty(aVal) && isEmpty(bVal)) {
    return 0
  }
  if (isEmpty(aVal)) {
    return 1
  }
  if (isEmpty(bVal)) {
    return -1
  }
  return compareValues(aVal, bVal, sorting)
}

function doSort(sorting, collection) {
  collection.comparator = function(a, b) {
    var sortValue = 0
    for (var i = 0; i <= sorting.length - 1; i++) {
      var sortField = sorting[i].attribute
      var sortOrder = sorting[i].direction === 'descending' ? -1 : 1
      switch (sortField) {
        case 'RELEVANCE':
          sortValue = sortOrder * (a.get('relevance') - b.get('relevance'))
          break
        case 'DISTANCE':
          sortValue = sortOrder * (a.get('distance') - b.get('distance'))
          break
        default:
          sortValue = checkSortValue(a, b, sorting[i])
      }
      if (sortValue !== 0) {
        break
      }
    }
    return sortValue
  }
  collection.sort()
}

module.exports = {
  sortResults: doSort,
}
