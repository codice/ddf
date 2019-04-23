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

const Marionette = require('marionette')
const _ = require('underscore')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('./property.view')
const PropertyCollection = require('./property.collection')
const properties = require('../../js/properties.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const announcement = require('../announcement/index.jsx')
const Common = require('../../js/Common.js')
const user = require('../singletons/user-instance.js')

function fallbackComparator(a, b) {
  a = metacardDefinitions.getLabel(a).toLowerCase()
  b = metacardDefinitions.getLabel(b).toLowerCase()
  if (a < b) {
    return -1
  }
  if (a > b) {
    return 1
  }
  return 0
}

module.exports = Marionette.CollectionView.extend(
  {
    tagName: CustomElements.register('property-collection'),
    childView: PropertyView,
    updateSort: function() {
      this.collection.sort()
    },
    addProperties: function(attributes) {
      const newAttributes = attributes.filter(
        attribute => !this.collection.get(attribute)
      );
      if (newAttributes.length > 0) {
        this.collection.add(
          newAttributes.map(attribute => {
            return {
              enumFiltering: true,
              enum: metacardDefinitions.enums[attribute],
              validation: metacardDefinitions.validation[attribute],
              label: properties.attributeAliases[attribute],
              readOnly: properties.isReadOnly(attribute),
              id: attribute,
              type: metacardDefinitions.metacardTypes[attribute].type,
              values: {},
              initializeToDefault: true,
              multivalued:
                metacardDefinitions.metacardTypes[attribute].multivalued,
            }
          })
        )
        this.children
          .findByModel(this.collection.get(newAttributes[0]))
          .el.scrollIntoView()
      }
      return newAttributes
    },
    removeProperties: function(attributes) {
      this.collection.remove(attributes)
    },
    turnOnEditing: function() {
      this.children.forEach(function(childView) {
        childView.turnOnEditing()
      })
    },
    turnOffEditing: function() {
      this.children.forEach(function(childView) {
        childView.turnOffEditing()
      })
    },
    revert: function() {
      this.children.forEach(function(childView) {
        if (childView.hasChanged()) {
          childView.revert()
        }
      })
    },
    save: function() {
      this.children.forEach(function(childView) {
        childView.save()
      })
    },
    toJSON: function() {
      return this.children.reduce(function(attributeToVal, childView) {
        return _.extend(attributeToVal, childView.toJSON())
      }, {})
    },
    toPropertyJSON() {
      return {
        properties: this.children.reduce(function(attributeToVal, childView) {
          const json = childView.toJSON()
          const values = json.values
            .filter(n => n != null && n.length > 0)
            .filter(n => !Number.isNaN(n))
          return _.extend(attributeToVal, { [json.attribute]: values })
        }, {}),
      }
    },
    toPatchJSON: function(addedAttributes, removedAttributes) {
      const attributeArray = [];
      this.children.forEach(function(childView) {
        const isNew = addedAttributes.indexOf(childView.model.id) >= 0;
        const attribute = isNew ? childView.toJSON() : childView.toPatchJSON();
        if (attribute) {
          attributeArray.push(attribute)
        }
      })
      removedAttributes.forEach(function(attribute) {
        attributeArray.push({
          attribute: attribute,
          values: [],
        })
      })
      return attributeArray
    },
    toPatchPropertyJSON: function() {
      return {
        properties: this.children.reduce(function(attributeToVal, childView) {
          const json = childView.toPatchJSON()
          if (typeof json === 'undefined') {
            return attributeToVal
          } else {
            const values = json.values
              .filter(n => n != null)
              .filter(n => !Number.isNaN(n))
            return _.extend(attributeToVal, { [json.attribute]: json.values })
          }
        }, {}),
      }
    },
    clearValidation: function() {
      this.children.forEach(function(childView) {
        childView.clearValidation()
      })
    },
    updateValidation: function(validationReport) {
      const self = this;
      validationReport.forEach(function(attributeValidationReport) {
        self.children
          .filter(function(childView) {
            return (
              childView.model.get('id') === attributeValidationReport.attribute
            )
          })
          .forEach(function(childView) {
            childView.updateValidation(attributeValidationReport)
          })
      })
    },
    focus: function() {
      if (this.children.length > 0) {
        this.children.first().focus()
      }
    },
    hasBlankRequiredAttributes() {
      return this.children.some(function(propertyView) {
        return propertyView.model.isRequired() && propertyView.model.isBlank()
      })
    },
    showRequiredWarnings() {
      this.children.forEach(propertyView => {
        propertyView.showRequiredWarning()
      })
    },
    hideRequiredWarnings() {
      this.children.forEach(propertyView => {
        propertyView.hideRequiredWarning()
      })
    },
    isValid() {
      return this.children.every(function(propertyView) {
        return propertyView.isValid()
      })
    },
  },
  {
    //contains methods for generating property collection views from service responses
    generateSummaryPropertyCollectionView: function(metacards) {
      const PropertyCollectionView = this.generateCollectionView(metacards);
      PropertyCollectionView.collection.comparator = function(a, b) {
        let preferredHeader = user
          .get('user')
          .get('preferences')
          .get('inspector-summaryOrder');
        if (preferredHeader.length === 0) {
          preferredHeader = properties.summaryShow
        }
        const aIndex = preferredHeader.indexOf(a.id);
        const bIndex = preferredHeader.indexOf(b.id);
        if (aIndex === -1 && bIndex === -1) {
          return metacardDefinitions.attributeComparator(a.id, b.id)
        }
        if (aIndex === -1) {
          return 1
        }
        if (bIndex === -1) {
          return -1
        }
        if (aIndex < bIndex) {
          return -1
        }
        if (aIndex > bIndex) {
          return 1
        }
        return 0
      }
      PropertyCollectionView.collection.sort()
      PropertyCollectionView.listenTo(
        user.get('user').get('preferences'),
        'change:inspector-summaryOrder',
        PropertyCollectionView.updateSort
      )
      return PropertyCollectionView
    },
    generatePropertyCollectionView: function(metacards) {
      const PropertyCollectionView = this.generateCollectionView(metacards);
      PropertyCollectionView.collection.comparator = function(a, b) {
        const preferredHeader = user
          .get('user')
          .get('preferences')
          .get('inspector-detailsOrder');
        const aIndex = preferredHeader.indexOf(a.id);
        const bIndex = preferredHeader.indexOf(b.id);
        if (aIndex === -1 && bIndex === -1) {
          return metacardDefinitions.attributeComparator(a.id, b.id)
        }
        if (aIndex === -1) {
          return 1
        }
        if (bIndex === -1) {
          return -1
        }
        if (aIndex < bIndex) {
          return -1
        }
        if (aIndex > bIndex) {
          return 1
        }
        return 0
      }
      PropertyCollectionView.collection.sort()
      PropertyCollectionView.listenTo(
        user.get('user').get('preferences'),
        'change:inspector-detailsOrder',
        PropertyCollectionView.updateSort
      )
      return PropertyCollectionView
    },
    generateFilteredPropertyCollectionView: function(
      propertyNames,
      metacards,
      options
    ) {
      const propertyArray = [];
      propertyNames.forEach(function(property) {
        if (metacardDefinitions.metacardTypes.hasOwnProperty(property)) {
          propertyArray.push({
            enumFiltering: true,
            enum: metacardDefinitions.enums[property],
            validation: metacardDefinitions.validation[property],
            label: properties.attributeAliases[property],
            readOnly: metacardDefinitions.metacardTypes[property].readOnly,
            id: property,
            type: metacardDefinitions.metacardTypes[property].type,
            values: {},
            multivalued:
              metacardDefinitions.metacardTypes[property].multivalued,
            required: properties.requiredAttributes.includes(property),
            initializeToDefault: true,
            ...options,
          })
        }
      })
      return this.generateFilteredCollectionView(propertyArray, metacards)
    },
    /* Generates a collection view containing all properties from the metacard intersection */
    generateCollectionView: function(metacards) {
      const propertyIntersection = this.determinePropertyIntersection(metacards)

      const propertyArray = propertyIntersection.map(prop => ({
        enumFiltering: true,
        enum: metacardDefinitions.enums[prop],
        validation: metacardDefinitions.validation[prop],
        label: properties.attributeAliases[prop],
        readOnly: metacardDefinitions.metacardTypes[prop].readOnly,
        id: prop,
        type: metacardDefinitions.metacardTypes[prop].type,
        values: {},
        multivalued: metacardDefinitions.metacardTypes[prop].multivalued,
        required: false,
      }))

      return this.generateFilteredCollectionView(propertyArray, metacards)
    },
    /* Generates a collection view containing only properties in the propertyArray */
    generateFilteredCollectionView: function(propertyArray, metacards) {
      propertyArray.forEach(function(property) {
        metacards.forEach(function(metacard) {
          let value = metacard[property.id];
          const isDefined = value !== undefined;
          let hasConflictingDefinition = false
          if (isDefined) {
            if (!metacardDefinitions.metacardTypes[property.id].multivalued) {
              if (!Array.isArray(value)) {
                value = [value]
              } else {
                hasConflictingDefinition = true
              }
            } else if (!Array.isArray(value)) {
              hasConflictingDefinition = true
              value = [value]
            }
          } else {
            value = [value]
          }
          const key = isDefined ? value : Common.undefined;
          value.sort()
          property.value = value
          property.values[key] = property.values[key] || {
            value: isDefined ? value : [],
            hits: 0,
            ids: [],
            hasNoValue: !isDefined,
          }
          property.hasConflictingDefinition = hasConflictingDefinition
          property.values[key].ids.push(metacard.id)
          property.values[key].hits++
        })
        if (metacards.length > 1) {
          property.bulk = true
          if (Object.keys(property.values).length > 1) {
            property.value = []
          }
        }
      })
      return new this({
        collection: new PropertyCollection(propertyArray),
        reorderOnSort: true,
      })
    },
    determinePropertyIntersection: function(metacards) {
      const metacardTypes = metacards.reduce((types, metacard) => {
        if (types.indexOf(metacard['metacard-type']) === -1) {
          types.push(metacard['metacard-type'])
        }
        return types
      }, [])
      const typeIntersection = _.intersection.apply(
        _,
        metacardTypes
          .filter(
            type => metacardDefinitions.metacardDefinitions[type] !== undefined
          )
          .map(type =>
            Object.keys(metacardDefinitions.metacardDefinitions[type])
          )
      )
      const attributeKeys = metacards.map(function(metacard) {
        return Object.keys(metacard)
      })
      let propertyIntersection = _.intersection(
        _.union.apply(_, attributeKeys),
        typeIntersection
      )
      propertyIntersection = propertyIntersection.filter(function(property) {
        if (metacardDefinitions.metacardTypes[property]) {
          return (
            !properties.isHidden(property) &&
            !metacardDefinitions.isHiddenTypeExceptThumbnail(property)
          )
        } else {
          announcement.announce({
            title: 'Missing Attribute Definition',
            message:
              'Could not find information for ' +
              property +
              ' in definitions.  If this problem persists, contact your Administrator.',
            type: 'warn',
          })
          return false
        }
      })
      return propertyIntersection
    },
  }
)
