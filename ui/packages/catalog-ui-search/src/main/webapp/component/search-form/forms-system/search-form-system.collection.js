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
const $ = require('jquery')
const Backbone = require('backbone')
const SearchForm = require('../search-form')
const Common = require('../../../js/Common.js')

let systemTemplates = []
let promiseIsResolved = false
const templatePromiseSupplier = () =>
  $.ajax({
    type: 'GET',
    context: this,
    url: './internal/forms/query',
    contentType: 'application/json',
    success: function(data) {
      systemTemplates = data
      promiseIsResolved = true
    },
  })

let bootstrapPromise = templatePromiseSupplier()

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    doneLoading: false,
    systemSearchForms: [],
  },
  initialize: function() {
    if (promiseIsResolved === true) {
      this.addSystemForms()
      promiseIsResolved = false
    }
    bootstrapPromise.then(() => {
      this.addSystemForms()
      this.doneLoading()
    })
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'systemSearchForms',
      collectionType: Backbone.Collection.extend({
        model: SearchForm,
        initialize: function() {},
      }),
    },
  ],
  addSystemForms: function() {
    if (!this.isDestroyed) {
      systemTemplates.forEach(value => {
        if (this.checkIfSystem(value)) {
          this.addSearchForm(
            new SearchForm({
              item_class: 'test',
              createdOn: value.created,
              id: value.id,
              title: value.title,
              description: value.description,
              type: 'custom',
              filterTemplate: value.filterTemplate,
              accessIndividuals: value.accessIndividuals,
              accessAdministrators: value.accessAdministrators,
              accessGroups: value.accessGroups,
              createdBy: 'system',
              owner: 'system',
              querySettings: value.querySettings,
            })
          )
        }
      })
    }
  },
  getCollection: function() {
    return this.get('systemSearchForms')
  },
  addSearchForm: function(searchForm) {
    this.get('systemSearchForms').add(searchForm)
  },
  getDoneLoading: function() {
    return this.get('doneLoading')
  },
  checkIfSystem: function(template) {
    return template.creator === 'system'
  },
  doneLoading: function() {
    this.set('doneLoading', true)
  },
})
