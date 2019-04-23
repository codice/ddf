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

const $ = require('jquery');
const _ = require('underscore');
const Marionette = require('marionette');
const template = require('./input-with-param.hbs');
const InputView = require('../input.view');

module.exports = InputView.extend({
  template: template,
  className: 'is-with-param',
  getCurrentValue: function() {
    const text = this.$el.find('[type=text]').val();
    const param = parseInt(this.$el.find('[type=number]').val());
    return {
      value: text,
      distance: Math.max(1, param || 0),
    }
  },
  onAttach() {
    const width = this.$el
      .find('.param-label')
      .last()
      .outerWidth()
    this.$el.find('.text, .param').css('width', `calc(50% - ${width / 2}px)`)
    InputView.prototype.onAttach.call(this)
  },
  handleValue: function() {
    const value = this.model.getValue() || {
      value: undefined,
      distance: 2,
    };
    this.$el.find('[type=text]').val(value.value)
    this.$el.find('[type=number]').val(value.distance)
  },
  serializeData: function() {
    const value = this.model.getValue() || {
      value: undefined,
      distance: 2,
    };
    return _.extend(this.model.toJSON(), {
      text: value.value,
      param: value.distance,
      label: this.model.get('property').get('param'),
      help: this.model.get('property').get('help'),
    })
  },
})
