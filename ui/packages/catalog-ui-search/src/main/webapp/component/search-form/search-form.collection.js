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
const $ = require('jquery')
const Backbone = require('backbone')
const SearchForm = require('./search-form')
const EventSourceUtil = require('../../js/EventSourceUtil')

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
  $.ajax({
    type: 'GET',
    context: this,
    url: './internal/forms/query',
    contentType: 'application/json',
    success(data) {
      fixTemplates(data)
      console.log('DATA:')
      console.log(data)
      cachedTemplates = data
      promiseIsResolved = true
    },
  })

let bootstrapPromise = templatePromiseSupplier()

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    doneLoading: false,
    searchForms: [],
  },
  initialize() {
    if (promiseIsResolved === true) {
      this.addAllForms()
      promiseIsResolved = false
      bootstrapPromise = new templatePromiseSupplier()
    }
    bootstrapPromise.then(() => {
      this.addAllForms()
      this.doneLoading()
    })

    let self = this

    EventSourceUtil.createEventListener('search', {
      onMessage: event => {
        console.log('SEARCH FORM: SSE ON MESSAGE')
        console.log(event.data)
        if (promiseIsResolved === true) {
          self.addAllForms(self)
          promiseIsResolved = false
          bootstrapPromise = new templatePromiseSupplier()
        }
        bootstrapPromise.then(() => {
          console.log('bootstrap promise')
          self.addAllForms(self)
          self.doneLoading(self)
        })
      },
    })
    // console.log('IN SEARCH FORM COLLECTION. SOURCE ID: ', id)
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'searchForms',
      collectionType: Backbone.Collection.extend({
        model: SearchForm,
        url: './internal/forms/query',
        initialize() {},
        comparator(a, b) {
          const titleA = a.get('title') || ''
          const titleB = b.get('title') || ''
          return titleA.toLowerCase().localeCompare(titleB.toLowerCase())
        },
      }),
    },
  ],
  addAllForms() {
    if (!this.isDestroyed) {
      const formsToDelete = this.get('searchForms')
        .map(form => {
          return cachedTemplates.every(
            template => template.id !== form.get('id')
          )
            ? form
            : null
        })
        .filter(form => form !== null)

      const formsToAdd = cachedTemplates
        .map(template => {
          return this.get('searchForms').every(
            form => form.get('id') !== template.id
          )
            ? template
            : null
        })
        .filter(template => template !== null)

      this.get('searchForms').remove(formsToDelete)

      formsToAdd.forEach((value, index) => {
        this.addSearchForm(
          new SearchForm({
            createdOn: value.created,
            id: value.id,
            title: value.title,
            description: value.description,
            type: 'custom',
            filterTemplate: value.filterTemplate,
            accessIndividuals: value.accessIndividuals,
            accessIndividualsRead: value.accessIndividualsRead,
            accessAdministrators: value.accessAdministrators,
            accessGroups: value.accessGroups,
            accessGroupsRead: value.accessGroupsRead,
            createdBy: value.creator,
            owner: value.owner,
            querySettings: value.querySettings,
          })
        )
      })
    }
    this.get('searchForms').sort()
  },
  getCollection() {
    return this.get('searchForms')
  },
  addSearchForm(searchForm) {
    this.get('searchForms').add(searchForm, { merge: true })
  },
  getDoneLoading() {
    return this.get('doneLoading')
  },
  doneLoading() {
    this.set('doneLoading', true)
  },
  deleteCachedTemplateById(id) {
    cachedTemplates = _.filter(cachedTemplates, template => template.id !== id)
  },
})
