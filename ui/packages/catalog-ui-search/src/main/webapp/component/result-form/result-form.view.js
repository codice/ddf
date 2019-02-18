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
/* global setTimeout */
const Marionette = require('marionette')
const $ = require('jquery')
const template = require('./result-form.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const Loading = require('../loading-companion/loading-companion.view.js')
const _ = require('underscore')
const announcement = require('../announcement/index.jsx')
const ResultFormsCollection = require('../result-form/result-form-collection-instance.js')
const Common = require('../../js/Common.js')
const properties = require('../../js/properties.js')
const ResultForm = require('../search-form/search-form.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('result-form'),
  modelEvents: {},
  events: {
    'click .editor-edit': 'edit',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
  },
  regions: {
    basicTitle: '.basic-text',
    basicDescription: '.basic-description',
    basicAttribute: '.basic-type',
    basicAttributeSpecific: '.basic-type-specific',
  },
  filter: undefined,
  onBeforeShow: function() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
    this.setupTitleInput()
    this.setupDescription()
    this.setupAttributeSpecific()
    this.edit()
  },
  setupAttributeSpecific: function() {
    let excludedList = metacardDefinitions.getMetacardStartingTypes()
    this.basicAttributeSpecific.show(
      new PropertyView({
        model: new Property({
          enumFiltering: true,
          showValidationIssues: true,
          enumMulti: true,
          enum: _.filter(metacardDefinitions.sortedMetacardTypes, function(
            type
          ) {
            return !metacardDefinitions.isHiddenTypeExceptThumbnail(type.id)
          })
            .filter(function(type) {
              return !properties.isHidden(type.id)
            })
            .filter(function(type) {
              return !excludedList.hasOwnProperty(type.id)
            })
            .map(function(metacardType) {
              return {
                label: metacardType.alias || metacardType.id,
                value: metacardType.id,
              }
            }),
          values: this.model.get('descriptors'),
          value: [this.model.get('descriptors')],
          id: 'Attributes',
        }),
      })
    )
  },
  setupTitleInput: function() {
    this.basicTitle.show(
      new PropertyView({
        model: new Property({
          value: [this.model.get('title')],
          id: 'Title',
          placeholder: 'Result Form Title',
        }),
      })
    )
  },
  setupDescription: function() {
    this.basicDescription.show(
      new PropertyView({
        model: new Property({
          value: [this.model.get('description')],
          id: 'Description',
          placeholder: 'Result Form Description',
        }),
      })
    )
  },
  edit: function() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  cancel: function() {
    this.cleanup()
  },
  save: function() {
    let view = this
    Loading.beginLoading(view)
    let descriptors = this.basicAttributeSpecific.currentView.model.get(
      'value'
    )[0]
    let title = this.basicTitle.currentView.model.getValue()[0].trim()
    const titleEmpty = title === ''
    const attributesEmpty = descriptors.length < 1
    if (titleEmpty || attributesEmpty) {
      const $titleValidationElement = this.basicTitle.currentView.$el.find(
        '> .property-label .property-validation'
      )
      const $attributeValidationElement = this.basicAttributeSpecific.currentView.$el.find(
        '> .property-label .property-validation'
      )
      if (titleEmpty) {
        if (!attributesEmpty) {
          $attributeValidationElement.addClass('is-hidden')
        }
        this.showWarningSymbol(
          $titleValidationElement,
          'Name field cannot be blank'
        )
      }
      if (attributesEmpty) {
        if (!titleEmpty) {
          $titleValidationElement.addClass('is-hidden')
        }
        this.showWarningSymbol(
          $attributeValidationElement,
          'Select at least one attribute'
        )
      }
      Loading.endLoading(view)
      return
    }
    let description = this.basicDescription.currentView.model.getValue()[0]
    let id = this.model.get('id')

    this.model.set({
      descriptors: descriptors.flatten(),
      title: title,
      description: description,
    })

    this.updateResults()
  },
  showWarningSymbol: function($validationElement, message) {
    $validationElement
      .removeClass('is-hidden')
      .removeClass('is-warning')
      .addClass('is-error')
    $validationElement.attr('title', message)
  },
  updateResults: function() {
    var collection = ResultFormsCollection.getCollection()
    const options = {
      success: () => {
        this.successMessage()
      },
      error: () => {
        this.errorMessage()
      },
    }
    this.model.set('type', 'result')
    this.model.id
      ? this.model.save({}, options)
      : collection.create(this.model, options)
    this.cleanup()
  },
  successMessage: function() {
    announcement.announce({
      title: 'Success',
      message: 'Result form successfully saved',
      type: 'success',
    })
  },
  errorMessage: function() {
    announcement.announce({
      title: 'Error',
      message: 'Result form failed to save',
      type: 'error',
    })
  },
  cleanup: function() {
    this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox')
    Loading.endLoading(this)
  },
})
