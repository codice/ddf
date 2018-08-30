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
/*global require*/
const _ = require('underscore')
const $ = require('jquery')
const Backbone = require('backbone')
const SearchForm = require('./search-form')
const Common = require('js/Common')
const user = require('component/singletons/user-instance')
const properties = require('properties')

const fixFilter = function(filter) {
  if (filter.filters) {
    filter.filters.forEach(fixFilter)
  } else {
    filter.defaultValue = filter.defaultValue || ''
    filter.value = filter.value || filter.defaultValue
  }
}

const fixTemplates = function(templates) {
  templates.forEach(template => {
    return fixFilter(template.filterTemplate)
  })
}

let cachedTemplates = []
let promiseIsResolved = false

const templatePromiseSupplier = () =>
  properties.hasExperimentalEnabled()
    ? $.ajax({
        type: 'GET',
        context: this,
        url: './internal/forms/query',
        contentType: 'application/json',
        success: function(data) {
          fixTemplates(data)
          cachedTemplates = data
          promiseIsResolved = true
        },
      })
    : Promise.resolve()

let bootstrapPromise = templatePromiseSupplier()

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    doneLoading: false,
    searchForms: properties.hasExperimentalEnabled()
      ? [
          new SearchForm({ type: 'new-form' }),
          new SearchForm({ type: 'basic' }),
          new SearchForm({ type: 'text' }),
        ]
      : [new SearchForm({ type: 'basic' }), new SearchForm({ type: 'text' })],
  },
  initialize: function() {
    if (promiseIsResolved === true) {
      this.addCustomForms()
      promiseIsResolved = false
      bootstrapPromise = new templatePromiseSupplier()
    }
    bootstrapPromise.then(() => {
      this.addCustomForms()
      this.doneLoading()
    })
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'searchForms',
      collectionType: Backbone.Collection.extend({
        model: SearchForm,
        initialize: function() {},
      }),
    },
  ],
  addCustomForms: function() {
    if (!this.isDestroyed) {
      cachedTemplates.forEach(
        function(value, index) {
          if (this.checkIfOwnerOrSystem(value)) {
            var utcSeconds = value.created / 1000
            var d = new Date(0)
            d.setUTCSeconds(utcSeconds)
            this.addSearchForm(
              new SearchForm({
                createdOn: Common.getHumanReadableDateTime(d),
                id: value.id,
                name: value.title,
                description: value.description,
                type: 'custom',
                filterTemplate: JSON.stringify(value.filterTemplate),
                accessIndividuals: value.accessIndividuals,
                accessGroups: value.accessGroups,
                createdBy: value.creator,
                owner: value.owner,
                querySettings: value.querySettings,
              })
            )
          }
        }.bind(this)
      )
    }
  },
  getCollection: function() {
    return this.get('searchForms')
  },
  addSearchForm: function(searchForm) {
    this.get('searchForms').add(searchForm, { merge: true })
  },
  getDoneLoading: function() {
    return this.get('doneLoading')
  },
  checkIfOwnerOrSystem: function(template) {
    let myEmail = user.get('user').get('email')
    let templateCreator = template.creator
    return myEmail === templateCreator || templateCreator === 'system'
  },
  doneLoading: function() {
    this.set('doneLoading', true)
  },
  deleteCachedTemplateById: function(id) {
    cachedTemplates = _.filter(cachedTemplates, function(template) {
      return template.id !== id
    })
  },
})
