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
/*global define, alert*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./value.hbs')
const CustomElements = require('js/CustomElements')
const InputView = require('component/input/input.view')
const InputThumbnailView = require('component/input/thumbnail/input-thumbnail.view')
const InputDateView = require('component/input/date/input-date.view')
const InputTimeView = require('component/input/time/input-time.view')
const InputLocationView = require('component/input/location/input-location.view')
const InputEnumView = require('component/input/enum/input-enum.view')
const InputRadioView = require('component/input/radio/input-radio.view')
const InputNumberView = require('component/input/number/input-number.view')
const InputBooleanView = require('component/input/boolean/input-boolean.view')
const InputRangeView = require('component/input/range/input-range.view')
const InputTextareaView = require('component/input/textarea/input-textarea.view')
const InputGeometryView = require('component/input/geometry/input-geometry.view')
const InputAutocompleteView = require('component/input/autocomplete/input-autocomplete.view')
const InputColorView = require('component/input/color/input-color.view')
const InputWithParamView = require('component/input/with-param/input-with-param.view')
const InputPasswordView = require('component/input/password/input-password.view')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('value'),
  events: {
    'click .value-delete': 'delete',
  },
  regions: {
    input: '.value-input',
  },
  initialize: function() {
    this.listenTo(
      this.model.get('property'),
      'change:isEditing',
      this.handleEdit
    )
  },
  onRender: function() {
    this.handleEdit()
    this.handleMultivalue()
  },
  onBeforeShow: function() {
    if (this.model.get('property').get('enum')) {
      this.input.show(
        new InputEnumView({
          model: this.model,
        })
      )
    } else if (this.model.get('property').get('radio')) {
      this.input.show(
        new InputRadioView({
          model: this.model,
        })
      )
    } else {
      switch (this.model.get('property').get('calculatedType')) {
        case 'date':
          this.input.show(
            new InputDateView({
              model: this.model,
            })
          )
          break
        case 'time':
          this.input.show(
            new InputTimeView({
              model: this.model,
            })
          )
          break
        case 'thumbnail':
          this.input.show(
            new InputThumbnailView({
              model: this.model,
            })
          )
          break
        case 'location':
          this.input.show(
            new InputLocationView({
              model: this.model,
            })
          )
          break
        case 'boolean':
          this.input.show(
            new InputBooleanView({
              model: this.model,
            })
          )
          break
        case 'geometry':
          this.input.show(
            new InputGeometryView({
              model: this.model,
            })
          )
          break
        case 'number':
          this.input.show(
            new InputNumberView({
              model: this.model,
            })
          )
          break
        case 'range':
          this.input.show(
            new InputRangeView({
              model: this.model,
            })
          )
          break
        case 'textarea':
          this.input.show(
            new InputTextareaView({
              model: this.model,
            })
          )
          break
        case 'autocomplete':
          this.input.show(
            new InputAutocompleteView({
              model: this.model,
            })
          )
          break
        case 'color':
          this.input.show(
            new InputColorView({
              model: this.model,
            })
          )
          break
        case 'near':
          this.input.show(
            new InputWithParamView({
              model: this.model,
            })
          )
          break
        case 'password':
          this.input.show(
            new InputPasswordView({
              model: this.model,
            })
          )
          break
        default:
          this.input.show(
            new InputView({
              model: this.model,
            })
          )
          break
      }
    }
  },
  handleEdit: function() {
    this.$el.toggleClass('is-editing', this.model.isEditing())
  },
  handleMultivalue: function() {
    this.$el.toggleClass('is-multivalued', this.model.isMultivalued())
  },
  focus: function() {
    this.input.currentView.focus()
  },
  delete: function() {
    this.model.destroy()
  },
})
