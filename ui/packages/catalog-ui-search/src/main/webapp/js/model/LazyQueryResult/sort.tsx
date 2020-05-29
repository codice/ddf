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

import { LazyQueryResult } from './LazyQueryResult'
import { QuerySortType } from './types'

const metacardDefinitions = require('../../../component/singletons/metacard-definitions.js')

function parseMultiValue(value: any) {
  if (value && value.constructor === Array) {
    return value[0]
  }
  return value
}

function isEmpty(value: any) {
  return value === undefined || value === null
}

function parseValue(value: any, attribute: string) {
  const attributeDefinition = metacardDefinitions.metacardTypes[attribute]
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

function compareValues(aVal: any, bVal: any, sorting: QuerySortType) {
  const sortOrder = sorting.direction === 'descending' ? -1 : 1
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

function checkSortValue(
  a: LazyQueryResult,
  b: LazyQueryResult,
  sorting: QuerySortType
) {
  const aVal = parseMultiValue(a.plain.metacard.properties[sorting.attribute])
  const bVal = parseMultiValue(b.plain.metacard.properties[sorting.attribute])
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

export const generateCompareFunction = (sorting: QuerySortType[]) => {
  return function(a: LazyQueryResult, b: LazyQueryResult) {
    let sortValue = 0
    for (let i = 0; i <= sorting.length - 1; i++) {
      const sortField = sorting[i].attribute
      const sortOrder = sorting[i].direction === 'descending' ? -1 : 1
      switch (sortField) {
        case 'RELEVANCE':
          sortValue = sortOrder * (a.plain.relevance - b.plain.relevance)
          break
        case 'DISTANCE':
          // this says distance could be null, could be a bug we need to address
          //@ts-ignore
          sortValue = sortOrder * (a.plain.distance - b.plain.distance)
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
}
