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
/*global define*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./sort-item.hbs')
const CustomElements = require('../../js/CustomElements.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const DropdownView = require('../dropdown/dropdown.view.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const properties = require('../../js/properties.js')

var blacklist = ['anyText', 'anyGeo']

function getSortLabel(attribute) {
  var ascendingLabel, descendingLabel
  if (metacardDefinitions.metacardTypes[attribute] === undefined) {
    ascendingLabel = descendingLabel = ''
  } else {
    switch (metacardDefinitions.metacardTypes[attribute].type) {
      case 'DATE':
        ascendingLabel = 'Earliest'
        descendingLabel = 'Latest'
        break
      case 'BOOLEAN':
        ascendingLabel = 'True First' //Truthiest
        descendingLabel = 'False First' //Falsiest
        break
      case 'LONG':
      case 'DOUBLE':
      case 'FLOAT':
      case 'INTEGER':
      case 'SHORT':
        ascendingLabel = 'Smallest'
        descendingLabel = 'Largest'
        break
      case 'STRING':
        ascendingLabel = 'A to Z'
        descendingLabel = 'Z to A'
        break
      case 'GEOMETRY':
        ascendingLabel = 'Closest'
        descendingLabel = 'Furthest'
        break
      case 'XML':
      case 'BINARY':
      default:
        ascendingLabel = 'Ascending'
        descendingLabel = 'Descending'
        break
    }
  }
  return {
    ascending: ascendingLabel,
    descending: descendingLabel,
  }
}

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('sort-item'),
  sortAttributes: [],
  regions: {
    sortAttribute: '.sort-attribute',
    sortDirection: '.sort-direction',
  },
  events: {
    'click .sort-remove': 'removeModel',
    'click .sort-add': 'addModel',
  },
  initialize: function(options) {
    this.index = options.childIndex
    this.collection = options.collection
  },
  removeModel: function() {
    this.model.destroy()
    this.destroy()
  },
  onBeforeShow: function() {
    this.sortAttributes = metacardDefinitions.sortedMetacardTypes
      .filter(function(type) {
        return !properties.isHidden(type.id)
      })
      .filter(function(type) {
        return !metacardDefinitions.isHiddenTypeExceptThumbnail(type.id)
      })
      .filter(function(type) {
        return blacklist.indexOf(type.id) === -1
      })
      .map(function(metacardType) {
        return {
          label: metacardType.alias || metacardType.id,
          value: metacardType.id,
        }
      })

    if (this.options.showBestTextOption) {
      this.sortAttributes.unshift({
        label: 'Best Text Match',
        value: 'RELEVANCE',
      })
    }

    this.sortAttribute.show(
      new PropertyView({
        model: new Property({
          enum: this.sortAttributes,

          value: [this.model.get('attribute')],
          id: 'Sort',
          enumFiltering: true,
          showLabel: !(this.index > 0),
        }),
      })
    )
    this.handleAttribute()
    this.turnOnEditing()

    this.listenTo(
      this.sortAttribute.currentView.model,
      'change:value',
      (model, attribute) => {
        this.model.set('attribute', attribute[0])
        this.handleAttribute()
      }
    )
    this.listenTo(this.collection, 'change:attribute remove', () => {
      this.updateDuplicates()
    })
  },
  updateDuplicates: function() {
    const hasDuplicates =
      this.collection.models.filter(sort => {
        return (
          sort.get('attribute') === this.model.get('attribute') &&
          sort.cid !== this.model.cid
        )
      }).length > 0

    this.$el.toggleClass('sort-duplicate-show', hasDuplicates)
  },
  turnOffEditing: function() {
    this.sortAttribute.currentView.turnOffEditing()
    this.sortDirection.currentView.turnOffEditing()
  },
  turnOnEditing: function() {
    this.sortAttribute.currentView.turnOnEditing()
    if (!this.$el.hasClass('is-non-directional-sort')) {
      this.sortDirection.currentView.turnOnEditing()
    }
  },
  handleAttribute: function() {
    if (!this.sortAttribute) {
      return
    }
    var attribute = this.sortAttribute.currentView.model.getValue()[0]
    var labels = getSortLabel(attribute)

    if (this.sortDirection.currentView !== undefined) {
      this.model.set(
        'direction',
        this.sortDirection.currentView.model.getValue()[0]
      )
    }

    this.sortDirection.show(
      new PropertyView({
        model: new Property({
          enum: [
            {
              label: labels.ascending,
              value: 'ascending',
            },
            {
              label: labels.descending,
              value: 'descending',
            },
          ],

          value: [this.model.get('direction')],
          id: 'Sort Direction',
          showLabel: false,
        }),
      })
    )

    if (metacardDefinitions.metacardTypes[attribute] === undefined) {
      this.sortDirection.currentView.turnOffEditing()
      this.$el.toggleClass('is-non-directional-sort', true)
    } else {
      this.sortDirection.currentView.turnOnEditing()
      this.$el.toggleClass('is-non-directional-sort', false)
    }

    this.listenTo(
      this.sortDirection.currentView.model,
      'change:value',
      (model, direction) => {
        this.model.set('direction', direction[0])
      }
    )
    this.updateDuplicates()
  },
  getSortField: function() {
    return this.sortAttribute.currentView.model.getValue()[0]
  },
  getSortOrder: function() {
    return this.sortDirection.currentView.model.getValue()[0]
  },
  serializeData: function() {
    const data = this.model.toJSON()
    data.aliased = metacardDefinitions.getLabel(data.attribute)
    if (data.aliased === 'RELEVANCE') {
      data.aliased = 'Best Text Match'
    }
    data.top = this.index === 0
    return data
  },
})
