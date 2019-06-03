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

const Marionette = require('marionette')
const _ = require('underscore')
const InputTemplate = require('./input.hbs')
const CustomElements = require('../../js/CustomElements.js')

const InputView = Marionette.LayoutView.extend({
  className() {
    if (!this.model.get('property').get('enum')) {
      return 'is-' + this.model.getCalculatedType()
    } else {
      return 'is-enum'
    }
  },
  template: InputTemplate,
  tagName: CustomElements.register('input'),
  attributes() {
    return {
      'data-id': this.model.getId(),
    }
  },
  modelEvents: {
    'change:isEditing': 'handleEdit',
  },
  regions: {},
  initialize() {
    if (this.model.get('property')) {
      this.listenTo(
        this.model.get('property'),
        'change:isEditing',
        this.handleEdit
      )
      this.listenTo(this.model, 'change:isValid', this.handleValidation)
    }
  },
  serializeData() {
    return _.extend(this.model.toJSON(), { cid: this.cid })
  },
  onRender() {
    this.handleEdit()
    this.handleReadOnly()
    this.handleValue()
    this.validate()
  },
  onAttach() {
    this.listenForChange()
  },
  listenForChange() {
    this.$el.on('change keyup input', () => {
      this.model.set('value', this.getCurrentValue())
      this.validate()
    })
  },
  validate() {
    if (this.model.get('property')) {
      this.model.setIsValid(this.isValid())
    }
  },
  handleValidation() {
    if (this.model.showValidationIssues()) {
      this.$el.toggleClass('has-validation-issues', !this.model.isValid())
    }
  },
  isValid() {
    return true //overwrite on a per input basis
  },
  handleReadOnly() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleEdit() {
    this.$el.toggleClass('is-editing', this.model.isEditing())
  },
  handleValue() {
    this.$el.find('input').val(this.model.getValue())
  },
  toJSON() {
    const attributeToVal = {}
    attributeToVal[this.model.getId()] = this.model.getValue()
    return attributeToVal
  },
  focus() {
    this.$el.find('input').select()
  },
  hasChanged() {
    const value = this.$el.find('input').val()
    return value !== this.model.getInitialValue()
  },
  getCurrentValue() {
    return this.$el.find('input').val()
  },
})

module.exports = InputView
