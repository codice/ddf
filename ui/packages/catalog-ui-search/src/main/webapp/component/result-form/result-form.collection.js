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
/* global require */
const Backbone = require('backbone')
const ResultForm = require('../search-form/search-form.js')
const Common = require('../../js/Common.js')
const user = require('../singletons/user-instance.js')
const $ = require('jquery')
const _ = require('underscore')

let resultTemplates = []
let promiseIsResolved = false
const resultTemplatePromise = () =>
  $.ajax({
    type: 'GET',
    url: './internal/forms/result',
    contentType: 'application/json',
    success: function(data) {
      resultTemplates = data
      promiseIsResolved = true
    },
  })
let bootstrapPromise = resultTemplatePromise()

module.exports = Backbone.AssociatedModel.extend({
  model: ResultForm,
  defaults: {
    doneLoading: false,
    added: false,
    resultForms: [],
  },
  initialize: function() {
    this.addResultForm(
      new ResultForm({
        title: 'Create New Data View',
        type: 'new-result',
      })
    )
    this.addResultForms()
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'resultForms',
      collectionType: Backbone.Collection.extend({
        model: ResultForm,
        url: './internal/forms/result',
        initialize: function() {},
      }),
    },
  ],
  addResultForms: function() {
    this.set('doneLoading', false)
    if (!this.isDestroyed) {
      if (promiseIsResolved === true) {
        promiseIsResolved = false
        bootstrapPromise = new resultTemplatePromise()
      }
      bootstrapPromise.then(() => {
        this.filteredList = _.map(resultTemplates, function(resultForm) {
          return {
            label: resultForm.title,
            value: resultForm.title,
            id: resultForm.id,
            descriptors: resultForm.descriptors,
            description: resultForm.description,
            owner: resultForm.owner,
            created: resultForm.created,
            creator: resultForm.creator,
            createdBy: resultForm.creator,
            accessGroups: resultForm.accessGroups,
            accessIndividuals: resultForm.accessIndividuals,
            accessAdministrators: resultForm.accessAdministrators,
          }
        })
        this.resetResultForm()
        this.addResultForm(
          new ResultForm({
            title: 'Create New Data View',
            type: 'new-result',
          })
        )
        this.filteredList.forEach(element => {
          this.addResultForm(
            new ResultForm({
              createdOn: element.created,
              id: element.id,
              title: element.label,
              type: 'result',
              descriptors: element.descriptors,
              owner: element.owner,
              accessIndividuals: element.accessIndividuals,
              accessAdministrators: element.accessAdministrators,
              accessGroups: element.accessGroups,
              createdBy: element.createdBy,
              description: element.description,
            })
          )
        })
        this.doneLoading()
      })
    }
  },
  addResultForm: function(newForm) {
    this.get('resultForms').add(newForm)
  },
  resetResultForm: function() {
    this.get('resultForms').reset()
  },
  getDoneLoading: function() {
    return this.get('doneLoading')
  },
  toggleUpdate: function() {
    this.set('added', !this.get('added'))
  },
  doneLoading: function() {
    this.set('doneLoading', true)
  },
  getCollection: function() {
    return this.get('resultForms')
  },
  deleteCachedTemplateById: function(id) {
    if (this.filteredList) {
      this.filteredList = _.filter(this.filteredList, function(template) {
        return template.id !== id
      })
      this.toggleUpdate()
    }
    if (resultTemplates) {
      resultTemplates = _.filter(resultTemplates, function(template) {
        return template.id !== id
      })
    }
  },
})
