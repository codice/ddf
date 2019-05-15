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
const template = require('./value.hbs')
const CustomElements = require('../../js/CustomElements.js')
const InputView = require('../input/input.view.js')
const InputThumbnailView = require('../input/thumbnail/input-thumbnail.view.js')
const InputDateView = require('../input/date/input-date.view.js')
const InputTimeView = require('../input/time/input-time.view.js')
const InputLocationView = require('../input/location/input-location.view.js')
const InputEnumView = require('../input/enum/input-enum.view.js')
const InputRadioView = require('../input/radio/input-radio.view.js')
const InputNumberView = require('../input/number/input-number.view.js')
const InputBooleanView = require('../input/boolean/input-boolean.view.js')
const InputRangeView = require('../input/range/input-range.view.js')
const InputTextareaView = require('../input/textarea/input-textarea.view.js')
const InputGeometryView = require('../input/geometry/input-geometry.view.js')
const InputAutocompleteView = require('../input/autocomplete/input-autocomplete.view.js')
const InputColorView = require('../input/color/input-color.view.js')
const InputWithParamView = require('../input/with-param/input-with-param.view.js')
const InputPasswordView = require('../input/password/input-password.view.js')

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
