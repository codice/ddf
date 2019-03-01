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
/*global define, setTimeout*/
const Marionette = require('marionette')
const memoize = require('lodash/memoize')
const $ = require('jquery')
const template = require('./query-advanced.hbs')
const CustomElements = require('../../js/CustomElements.js')
const FilterBuilderView = require('../filter-builder/filter-builder.view.js')
const FilterBuilderModel = require('../filter-builder/filter-builder.js')
const cql = require('../../js/cql.js')
const store = require('../../js/store.js')
const QuerySettingsView = require('../query-settings/query-settings.view.js')
const user = require('../singletons/user-instance.js')
const properties = require('../../js/properties.js')

import query from '../../react-component/utils/query'

const fetchSuggestions = memoize(async attr => {
  const json = await query({
    count: 0,
    cql: "anyText ILIKE ''",
    facets: [attr],
  })

  const suggestions = json.facets[attr]

  if (suggestions === undefined) {
    return []
  }

  suggestions.sort((a, b) => b.count - a.count)

  return suggestions.map(({ value }) => value)
})

const isValidFacetAttribute = (id, type) => {
  if (!['STRING', 'INTEGER', 'FLOAT'].includes(type)) {
    return false
  }
  if (id === 'anyText') {
    return false
  }
  if (!properties.facetWhitelist.includes(id)) {
    return false
  }
  return true
}

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('query-advanced'),
  modelEvents: {},
  events: {
    'click .editor-edit': 'edit',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
  },
  regions: {
    querySettings: '.query-settings',
    queryAdvanced: '.query-advanced',
  },
  ui: {},
  initialize: function() {
    this.$el.toggleClass('is-form-builder', this.options.isFormBuilder === true)
    this.$el.toggleClass('is-form', this.options.isForm === true)
    this.listenTo(
      user.getQuerySettings(),
      'change:template',
      this.handleTemplateChange
    )
    this.handleTemplateChange()
  },
  handleTemplateChange: function() {
    let userDefaultTemplate = user.getQuerySettings().get('template')
    if (!userDefaultTemplate) {
      return
    }
    let sorts =
      userDefaultTemplate['querySettings'] &&
      userDefaultTemplate['querySettings'].sorts
    if (sorts) {
      sorts = sorts.map(sort => ({
        attribute: sort.split(',')[0],
        direction: sort.split(',')[1],
      }))
    }
    this.model.set({
      type: 'custom',
      title: userDefaultTemplate['title'],
      filterTree: userDefaultTemplate['filterTemplate'],
      src:
        (userDefaultTemplate['querySettings'] &&
          userDefaultTemplate['querySettings'].src) ||
        '',
      federation:
        (userDefaultTemplate['querySettings'] &&
          userDefaultTemplate['querySettings'].federation) ||
        'enterprise',
      sorts: sorts,
      'detail-level':
        (userDefaultTemplate['querySettings'] &&
          userDefaultTemplate['querySettings']['detail-level']) ||
        'allFields',
    })
    if (
      this.options.isForm === true &&
      this.model.get('filterTree') !== undefined &&
      this.queryAdvanced.currentView !== undefined
    ) {
      this.queryAdvanced.currentView.deserialize(this.model.get('filterTree'))
    }
  },
  onBeforeShow: function() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
    this.querySettings.show(
      new QuerySettingsView({
        model: this.model,
        isForm: this.options.isForm || false,
        isFormBuilder: this.options.isFormBuilder || false,
      })
    )
    this.queryAdvanced.show(
      new FilterBuilderView({
        model: new FilterBuilderModel(),
        suggester: async ({ id, type }) => {
          if (!isValidFacetAttribute(id, type)) {
            return []
          }

          return fetchSuggestions(id)
        },
        isForm: this.options.isForm || false,
        isFormBuilder: this.options.isFormBuilder || false,
      })
    )

    if (this.model.get('filterTree') !== undefined) {
      this.queryAdvanced.currentView.deserialize(this.model.get('filterTree'))
    } else if (this.options.isAdd) {
      this.queryAdvanced.currentView.deserialize(cql.read("anyText ILIKE '%'"))
    } else if (this.model.get('cql')) {
      this.queryAdvanced.currentView.deserialize(
        cql.simplify(cql.read(this.model.get('cql')))
      )
    }
    this.queryAdvanced.currentView.turnOffEditing()
    this.edit()
  },
  focus: function() {
    var tabbable = _.filter(
      this.$el.find('[tabindex], input, button'),
      function(element) {
        return element.offsetParent !== null
      }
    )
    if (tabbable.length > 0) {
      $(tabbable[0]).focus()
    }
  },
  edit: function() {
    this.$el.toggleClass(
      'is-empty',
      this.model.get('comparator') === 'IS EMPTY'
    )
    this.$el.addClass('is-editing')
    this.querySettings.currentView.turnOnEditing()
    this.queryAdvanced.currentView.turnOnEditing()
    if (this.options.isForm === true && this.options.isFormBuilder !== true) {
      this.queryAdvanced.currentView.turnOffEditing()
      //TODO: https://codice.atlassian.net/browse/DDF-3861 Deal with the oddities in turning off editing in that view
      //this.querySettings.currentView.turnOffEditing();
    }
  },
  cancel: function() {
    fetchSuggestions.cache.clear()
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
    if (typeof this.options.onCancel === 'function') {
      this.options.onCancel()
    }
  },
  save: function() {
    fetchSuggestions.cache.clear()
    if (!this.options.isSearchFormEditor) {
      this.$el.removeClass('is-editing')
    }
    this.querySettings.currentView.saveToModel()

    this.queryAdvanced.currentView.sortCollection()
    this.model.set({
      cql:
        this.options.isFormBuilder !== true
          ? this.queryAdvanced.currentView.transformToCql()
          : '',
      filterTree: cql.simplify(this.queryAdvanced.currentView.getFilters()),
    })
    if (typeof this.options.onSave === 'function') {
      this.options.onSave()
    }
  },
  isValid: function() {
    return this.querySettings.currentView.isValid()
  },
  setDefaultTitle: function() {
    this.model.set('title', this.model.get('cql'))
  },
  serializeTemplateParameters: function() {
    this.queryAdvanced.currentView.sortCollection()
    return {
      filterTree: this.queryAdvanced.currentView.getFilters(),
      filterSettings: this.querySettings.currentView.toJSON(),
    }
  },
})
