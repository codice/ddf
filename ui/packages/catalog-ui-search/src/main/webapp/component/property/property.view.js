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
/* eslint-disable no-var */

const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./property.hbs')
const CustomElements = require('../../js/CustomElements.js')
const BulkInputView = require('../input/bulk/input-bulk.view.js')
const Property = require('./property')

module.exports = Marionette.LayoutView.extend(
  {
    template,
    tagName: CustomElements.register('property'),
    attributes() {
      return {
        'data-id': this.model.get('id'),
        'data-label': this.model.get('label') || this.model.get('id'),
        title: this.model.get('title'),
      }
    },
    events: {
      'click .property-revert': 'revert',
      'keyup input': 'handleRevert',
      click: 'handleRevert',
      'dp.change': 'handleRevert',
      change: 'handleRevert',
    },
    modelEvents: {
      'change:hasChanged': 'handleRevert',
      'change:isEditing': 'handleEdit',
    },
    regions: {
      propertyValue: '.property-value',
    },
    serializeData() {
      return _.extend(this.model.toJSON(), { cid: this.cid })
    },
    initialize() {
      this.turnOnLimitedWidth()
      this.listenTo(
        this.model,
        'change:showRequiredWarning',
        this.handleShowRequired
      )
      this.listenTo(
        this.model,
        'change:value change:showRequiredWarning',
        this.validateRequired
      )
    },
    onRender() {
      this.handleEdit()
      this.handleReadOnly()
      this.handleRequired()
      this.handleConflictingAttributeDefinition()
      this.handleValue()
      this.handleRevert()
      this.handleValidation()
      this.handleLabel()
      this.handleOnlyEditing()
    },
    onBeforeShow() {
      this.propertyValue.show(
        new BulkInputView({
          model: this.model,
        })
      )
    },
    hide() {
      this.$el.toggleClass('is-hidden', true)
    },
    show() {
      this.$el.toggleClass('is-hidden', false)
    },
    handleOnlyEditing() {
      this.$el.toggleClass('only-editing', this.model.onlyEditing())
    },
    handleLabel() {
      this.$el.toggleClass('hide-label', !this.model.showLabel())
    },
    handleConflictingAttributeDefinition() {
      this.$el.toggleClass(
        'has-conflicting-definitions',
        this.model.hasConflictingDefinitions()
      )
    },
    handleReadOnly() {
      this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
    },
    handleRequired() {
      if (this.model.isRequired()) {
        this.$el.addClass('is-required')
      } else {
        this.$el.removeClass('is-required')
      }
    },
    handleEdit() {
      this.$el.toggleClass('is-editing', this.model.get('isEditing'))
    },
    handleValue() {
      this.$el.find('input').val(this.model.getValue())
    },
    turnOnEditing() {
      if (
        !this.model.get('readOnly') &&
        !this.model.hasConflictingDefinitions()
      ) {
        this.model.set('isEditing', true)
      }
      this.$el.addClass('is-editing')
    },
    turnOffEditing() {
      this.model.set('isEditing', false)
      this.$el.removeClass('is-editing')
    },
    turnOnLimitedWidth() {
      this.$el.addClass('has-limited-width')
    },
    revert() {
      this.model.revert()
      this.onBeforeShow()
    },
    isValid() {
      return this.model.isValid()
    },
    showRequiredWarning() {
      this.model.showRequiredWarning()
    },
    hideRequiredWarning() {
      this.model.hideRequiredWarning()
    },
    save() {
      const value = this.$el.find('input').val()
      this.model.save(value)
    },
    toJSON() {
      const value = this.model.getValue()
      return {
        attribute: this.model.getId(),
        values: value,
      }
    },
    toPatchJSON() {
      if (this.hasChanged()) {
        return this.toJSON()
      } else {
        return undefined
      }
    },
    focus() {
      setTimeout(() => {
        this.$el.find('input').select()
      }, 0)
    },
    hasChanged() {
      return this.model.get('hasChanged')
    },
    handleRevert() {
      if (this.hasChanged()) {
        this.$el.addClass('is-changed')
      } else {
        this.$el.removeClass('is-changed')
      }
    },
    concatMessages(totalMessage, currentMessage) {
      totalMessage.push(currentMessage)
      return totalMessage
    },
    updateValidation(validationReport) {
      this._validationReport = validationReport
      const $validationElement = this.$el.find('.property-validation')
      if (validationReport.errors.length > 0) {
        this.$el.removeClass('has-warning').addClass('has-error')
        $validationElement
          .removeClass('is-hidden')
          .removeClass('is-warning')
          .addClass('is-error')
        var validationMessage = validationReport.errors.reduce(
          this.concatMessages,
          []
        )
        this.setMessage($validationElement, validationMessage)
      } else if (validationReport.warnings.length > 0) {
        this.$el.addClass('has-warning').removeClass('has-error')
        $validationElement
          .removeClass('is-hidden')
          .removeClass('is-error')
          .addClass('is-warning')
        var validationMessage = validationReport.warnings.reduce(
          this.concatMessages,
          []
        )
        this.setMessage($validationElement, validationMessage)
      }
      this.handleBulkValidation(validationReport)
    },
    handleBulkValidation(validationReport) {
      const elementsToCheck = this.$el.find(
        '.is-bulk > .if-viewing .list-value'
      )
      _.forEach(elementsToCheck, function(element) {
        if (
          $(element)
            .attr('data-ids')
            .split(',')
            .indexOf(validationReport.id) !== -1
        ) {
          const $validationElement = $(element).find('.cell-validation')
          if (validationReport.errors.length > 0) {
            $validationElement
              .removeClass('is-hidden')
              .removeClass('is-warning')
              .addClass('is-error')
            var validationMessage = validationReport.errors.reduce(
              this.concatMessages,
              []
            )
            this.setMessage($validationElement, validationMessage)
          } else if (validationReport.warnings.length > 0) {
            $validationElement
              .removeClass('is-hidden')
              .removeClass('is-error')
              .addClass('is-warning')
            var validationMessage = validationReport.warnings.reduce(
              this.concatMessages,
              []
            )
            this.setMessage($validationElement, validationMessage)
          }
        }
      })
    },
    setMessage(elements, message) {
      _.forEach(elements, el => {
        const element = $(el)
        if (element.is('div')) {
          let body = ''
          message.forEach(element => {
            body +=
              "<div><span class='fa fa-exclamation-triangle'></span> <span class='validation-message'>" +
              element +
              '</div>'
          })
          element.html(body)
        } else {
          element.attr('title', message.join(' '))
        }
      })
    },
    clearValidation() {
      let $validationElement = this.$el.find('.property-validation')
      this.$el.removeClass('has-warning').removeClass('has-error')
      $validationElement.addClass('is-hidden')

      const elementsToCheck = this.$el.find(
        '.is-bulk > .if-viewing .list-value'
      )
      _.forEach(elementsToCheck, element => {
        $validationElement = $(element).find('.cell-validation')
        $validationElement.removeClass('has-warning').removeClass('has-error')
        $validationElement.addClass('is-hidden')
      })
    },
    handleValidation() {
      if (this._validationReport) {
        this.updateValidation(this._validationReport)
      }
    },
    handleShowRequired() {
      this.$el.toggleClass(
        'show-required-warning',
        this.model.get('showRequiredWarning')
      )
    },
    validateRequired() {
      if (this.model.isRequired()) {
        if (this.model.isBlank()) {
          this.$el.addClass('failed-required-check')
        } else {
          this.$el.removeClass('failed-required-check')
        }
      }
    },
  },
  {
    getPropertyView(modelJSON) {
      return new this({
        model: new Property(modelJSON),
      })
    },
  }
)
