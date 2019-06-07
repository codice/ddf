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

const _ = require('underscore')
const Backbone = require('backbone')
const poller = require('backbone-poller')
const properties = require('../properties.js')
const $ = require('jquery')

function removeLocalCatalogIfNeeded(response, localCatalog) {
  if (properties.isDisableLocalCatalog()) {
    response = _.filter(response, source => source.id !== localCatalog)
  }

  return response
}

const Types = Backbone.Collection.extend({})

const computeTypes = function(sources) {
  if (_.size(properties.typeNameMapping) > 0) {
    return _.map(properties.typeNameMapping, (value, key) => {
      if (_.isArray(value)) {
        return {
          name: key,
          value: value.join(','),
        }
      }
    })
  } else {
    return _.chain(sources)
      .map(source => source.contentTypes)
      .flatten()
      .filter(element => element.name !== '')
      .sortBy(element => element.name.toUpperCase())
      .uniq(false, type => type.name)
      .map(element => {
        element.value = element.name
        return element
      })
      .value()
  }
}

module.exports = Backbone.Collection.extend({
  url: './internal/catalog/sources',
  useAjaxSync: true,
  comparator(a, b) {
    const aName = a.id.toLowerCase()
    const bName = b.id.toLowerCase()
    const aAvailable = a.get('available')
    const bAvailable = b.get('available')
    if ((aAvailable && bAvailable) || (!aAvailable && !bAvailable)) {
      if (aName < bName) {
        return -1
      }
      if (aName > bName) {
        return 1
      }
      return 0
    } else if (!aAvailable) {
      return -1
    } else if (!bAvailable) {
      return 1
    }
  },
  initialize() {
    this.listenTo(this, 'change', this.sort)
    this._types = new Types()
    this.determineLocalCatalog()
    this.listenTo(this, 'sync', this.updateLocalCatalog)
  },
  types() {
    return this._types
  },
  parse(response) {
    response = removeLocalCatalogIfNeeded(response, this.localCatalog)
    this._types.set(computeTypes(response))
    return response
  },
  determineLocalCatalog() {
    $.get('./internal/localcatalogid').then(data => {
      this.localCatalog = data['local-catalog-id']

      poller
        .get(this, {
          delay: properties.sourcePollInterval,
          delayed: properties.sourcePollInterval,
          continueOnError: true,
        })
        .start()

      this.fetch()
    })
  },
  updateLocalCatalog() {
    if (this.get(this.localCatalog)) {
      this.get(this.localCatalog).set('local', true)
    }
  },
  localCatalog: 'local',
})
