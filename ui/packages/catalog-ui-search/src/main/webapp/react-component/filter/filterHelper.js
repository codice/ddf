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
/* global _ */
const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
const properties = require('../../js/properties.js')
const MultivalueView = require('../../component/multivalue/multivalue.view.js')
const RelativeTimeView = require('../../component/relative-time/relative-time.view.js')
const BetweenTimeView = require('../../component/between-time/between-time.view.js')

export const generatePropertyJSON = (value, type, comparator) => {
  const propertyJSON = _.extend({}, metacardDefinitions.metacardTypes[type], {
    value,
    multivalued: false,
    enumFiltering: true,
    enumCustom: true,
    matchcase: ['MATCHCASE', '='].indexOf(comparator) !== -1 ? true : false,
    enum: metacardDefinitions.enums[type],
    showValidationIssues: false,
  })

  if (propertyJSON.type === 'GEOMETRY') {
    propertyJSON.type = 'LOCATION'
  }

  if (propertyJSON.type === 'STRING') {
    propertyJSON.placeholder = 'Use * for wildcard.'
  }

  if (comparator === 'NEAR' && propertyJSON.type === 'STRING') {
    propertyJSON.type = 'NEAR'
    propertyJSON.param = 'within'
    propertyJSON.help =
      'The distance (number of words) within which search terms must be found in order to match'
    delete propertyJSON.enum
  }

  // if we don't set this the property model will transform the value as if it's a date, clobbering the special format
  if (
    (comparator === 'RELATIVE' || comparator === 'BETWEEN') &&
    propertyJSON.type === 'DATE'
  ) {
    propertyJSON.transformValue = false
  }

  return propertyJSON
}

export const determineView = comparator => {
  let necessaryView
  switch (comparator) {
    case 'RELATIVE':
      necessaryView = RelativeTimeView
      break
    case 'BETWEEN':
      necessaryView = BetweenTimeView
      break
    default:
      necessaryView = MultivalueView
      break
  }
  return necessaryView
}

export const transformValue = (value, comparator) => {
  switch (comparator) {
    case 'NEAR':
      if (value[0].constructor !== Object) {
        value[0] = {
          value: value[0],
          distance: 2,
        }
      }
      break
    case 'INTERSECTS':
    case 'DWITHIN':
      break
    default:
      if (value === null || value[0] === null) {
        value = ['']
        break
      }
      if (value[0].constructor === Object) {
        value[0] = value[0].value
      }
      break
  }
  return value
}

export const getFilteredAttributeList = includedAttributes => {
  return metacardDefinitions.sortedMetacardTypes
    .filter(({ id }) => !properties.isHidden(id))
    .filter(({ id }) => !metacardDefinitions.isHiddenType(id))
    .filter(
      ({ id }) =>
        includedAttributes === undefined
          ? true
          : includedAttributes.includes(id)
    )
    .map(({ alias, id }) => ({
      label: alias || id,
      value: id,
      description: (properties.attributeDescriptions || {})[id],
    }))
}
