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

const $ = require('jquery')
const Backbone = require('backbone')
const _ = require('underscore')
const properties = require('../../js/properties.js')
const moment = require('moment')
function transformEnumResponse(metacardTypes, response) {
  return _.reduce(
    response,
    (result, value, key) => {
      switch (metacardTypes[key].type) {
        case 'DATE':
          result[key] = value.map(subval => {
            if (subval) {
              return moment(subval).toISOString()
            }
            return subval
          })
          break
        case 'LONG':
        case 'DOUBLE':
        case 'FLOAT':
        case 'INTEGER':
        case 'SHORT': //needed until enum response correctly returns numbers as numbers
          result[key] = value.map((
            subval //handle cases of unnecessary number padding -> 22.0000
          ) => Number(subval))
          break
        default:
          result[key] = value
          break
      }
      return result
    },
    {}
  )
}
const metacardStartingTypes = {
  anyText: {
    id: 'anyText',
    type: 'STRING',
    multivalued: false,
  },
  anyGeo: {
    id: 'anyGeo',
    type: 'LOCATION',
    multivalued: false,
  },
  'metacard-type': {
    id: 'metacard-type',
    type: 'STRING',
    multivalued: false,
    readOnly: true,
  },
  'source-id': {
    id: 'source-id',
    type: 'STRING',
    multivalued: false,
    readOnly: true,
  },
  cached: {
    id: 'cached',
    type: 'STRING',
    multivalued: false,
  },
  'metacard-tags': {
    id: 'metacard-tags',
    type: 'STRING',
    multivalued: true,
  },
}

// needed to handle erroneous or currently unknown attributes (they could be picked up after searching a source)
properties.basicSearchTemporalSelectionDefault.forEach(proposedType => {
  metacardStartingTypes[proposedType] = {
    id: proposedType,
    type: 'DATE',
    alias: properties.attributeAliases[proposedType],
    hidden: properties.isHidden(proposedType),
  }
})

module.exports = new (Backbone.Model.extend({
  initialize() {
    this.updateSortedMetacardTypes()
    this.getMetacardTypes()
    this.getDatatypeEnum()
  },
  isHiddenTypeExceptThumbnail(id) {
    if (id === 'thumbnail') {
      return false
    } else {
      return this.isHiddenType(id)
    }
  },
  isHiddenType(id) {
    return (
      this.metacardTypes[id] === undefined ||
      this.metacardTypes[id].type === 'XML' ||
      this.metacardTypes[id].type === 'BINARY' ||
      this.metacardTypes[id].type === 'OBJECT'
    )
  },
  getDatatypeEnum() {
    $.get('./internal/enumerations/attribute/datatype').then(response => {
      _.extend(this.enums, response)
    })
  },
  getEnumForMetacardDefinition(metacardDefinition) {
    $.get('./internal/enumerations/metacardtype/' + metacardDefinition).then(
      response => {
        _.extend(
          this.enums,
          transformEnumResponse(this.metacardTypes, response)
        )
      }
    )
  },
  addMetacardDefinition(metacardDefinitionName, metacardDefinition) {
    if (
      Object.keys(this.metacardDefinitions).indexOf(metacardDefinitionName) ===
      -1
    ) {
      this.getEnumForMetacardDefinition(metacardDefinitionName)
      this.metacardDefinitions[metacardDefinitionName] = metacardDefinition
      for (const type in metacardDefinition) {
        if (metacardDefinition.hasOwnProperty(type)) {
          this.metacardTypes[type] = metacardDefinition[type]
          this.metacardTypes[type].id = this.metacardTypes[type].id || type
          this.metacardTypes[type].type =
            this.metacardTypes[type].type || this.metacardTypes[type].format
          this.metacardTypes[type].alias = properties.attributeAliases[type]
          this.metacardTypes[type].hidden =
            properties.isHidden(this.metacardTypes[type].id) ||
            this.isHiddenTypeExceptThumbnail(this.metacardTypes[type].id)
          this.metacardTypes[type].readOnly = properties.isReadOnly(
            this.metacardTypes[type].id
          )
        }
      }
      return true
    }
    return false
  },
  addMetacardDefinitions(metacardDefinitions) {
    let updated = false
    for (const metacardDefinition in metacardDefinitions) {
      if (metacardDefinitions.hasOwnProperty(metacardDefinition)) {
        updated =
          this.addMetacardDefinition(
            metacardDefinition,
            metacardDefinitions[metacardDefinition]
          ) || updated
      }
    }
    if (updated) {
      this.updateSortedMetacardTypes()
    }
  },
  getMetacardTypes() {
    $.get('./internal/metacardtype').then(metacardDefinitions => {
      this.addMetacardDefinitions(metacardDefinitions)
    })
  },
  attributeComparator(a, b) {
    const attrToCompareA = this.getLabel(a).toLowerCase()
    const attrToCompareB = this.getLabel(b).toLowerCase()
    if (attrToCompareA < attrToCompareB) {
      return -1
    }
    if (attrToCompareA > attrToCompareB) {
      return 1
    }
    return 0
  },
  sortMetacardTypes(metacardTypes) {
    return metacardTypes.sort((a, b) => {
      const attrToCompareA = (a.alias || a.id).toLowerCase()
      const attrToCompareB = (b.alias || b.id).toLowerCase()
      if (attrToCompareA < attrToCompareB) {
        return -1
      }
      if (attrToCompareA > attrToCompareB) {
        return 1
      }
      return 0
    })
  },
  updateSortedMetacardTypes() {
    this.sortedMetacardTypes = []
    for (const propertyType in this.metacardTypes) {
      if (this.metacardTypes.hasOwnProperty(propertyType)) {
        this.sortedMetacardTypes.push(this.metacardTypes[propertyType])
      }
    }
    this.sortMetacardTypes(this.sortedMetacardTypes)
  },
  getLabel(id) {
    const definition = this.metacardTypes[id]
    return definition ? definition.alias || id : id
  },
  getMetacardStartingTypes() {
    return metacardStartingTypes
  },
  metacardDefinitions: [],
  sortedMetacardTypes: [],
  metacardTypes: _.extendOwn({}, metacardStartingTypes),
  validation: {},
  enums: properties.enums,
}))()
