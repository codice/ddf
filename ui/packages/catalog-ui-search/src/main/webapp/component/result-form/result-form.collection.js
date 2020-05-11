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
const $ = require('jquery')
const _ = require('underscore')

let resultTemplates = []
let promiseIsResolved = false
const resultTemplatePromise = () =>
  $.ajax({
    type: 'GET',
    context: this,
    url: './internal/forms/result',
    contentType: 'application/json',
    success(data) {
      resultTemplates = data
      promiseIsResolved = true
    },
  })
let bootstrapPromise = resultTemplatePromise()

module.exports = Backbone.AssociatedModel.extend({
  model: ResultForm,
  defaults: {
    doneLoading: false,
    resultForms: [],
  },
  initialize() {
    if (promiseIsResolved === true) {
      this.addResultForms()
      promiseIsResolved = false
      bootstrapPromise = new resultTemplatePromise()
    }
    bootstrapPromise.then(() => {
      this.addResultForms()
      this.doneLoading()
    })
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'resultForms',
      collectionType: Backbone.Collection.extend({
        model: ResultForm,
        url: './internal/forms/result',
        initialize() {},
        comparator(a, b) {
          const titleA = a.get('title') || ''
          const titleB = b.get('title') || ''
          return titleA.toLowerCase().localeCompare(titleB.toLowerCase())
        },
      }),
    },
  ],
  addResultForms() {
    if (!this.isDestroyed) {
      resultTemplates.forEach((value, index) => {
        this.addResultForm(
          new ResultForm({
            title: value.title,
            type: 'result',
            id: value.id,
            descriptors: value.descriptors,
            description: value.description,
            owner: value.owner,
            createdOn: value.created,
            createdBy: value.creator,
            accessGroups: value.accessGroups,
            accessIndividuals: value.accessIndividuals,
            accessAdministrators: value.accessAdministrators,
          })
        )
      })
      this.get('resultForms').sort()
    }
  },
  addResultForm(newForm) {
    this.get('resultForms').add(newForm)
  },
  resetResultForm() {
    this.get('resultForms').reset()
  },
  getDoneLoading() {
    return this.get('doneLoading')
  },
  doneLoading() {
    this.set('doneLoading', true)
  },
  getCollection() {
    return this.get('resultForms')
  },
  deleteCachedTemplateById(id) {
    if (resultTemplates) {
      resultTemplates = _.filter(
        resultTemplates,
        template => template.id !== id
      )
    }
  },
})
