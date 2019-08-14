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

const template = require('./input-textarea.hbs')
const InputView = require('../input.view')
const $ = require('jquery')

module.exports = InputView.extend({
  template,
  events: {
    'click .ta-expand': 'expand',
    'click .ta-contract': 'contract',
  },
  handleValue() {
    this.$el.find('textarea').text(this.model.getValue())
    this.hasOverflowed()
  },
  onRender() {
    InputView.prototype.onRender.call(this)
    this.hasOverflowed()
    this.addResizeHandler()
  },
  onAttach() {
    InputView.prototype.onAttach.call(this)
    this.hasOverflowed()
  },
  getCurrentValue() {
    return this.$el.find('textarea').val()
  },
  hasOverflowed() {
    const textarea = this.$el.find('.ta-disabled')
    const scrollableHeight = textarea.prop('scrollHeight')
    const currViewableHeight = parseInt(textarea.css('max-height'), 10)
    this.$el.toggleClass(
      'has-overflowed',
      scrollableHeight > currViewableHeight + 10
    )
  },
  expand() {
    const textarea = this.$el.find('.ta-disabled')
    const scrollableHeight = textarea.prop('scrollHeight')
    this.$el.toggleClass('is-expanded', true)
    textarea.css('height', scrollableHeight + 15)
    textarea.css('max-height', scrollableHeight + 15)
  },
  contract() {
    const textarea = this.$el.find('.ta-disabled')
    this.$el.toggleClass('is-expanded', false)
    textarea.css('height', '75px')
    textarea.css('max-height', '75px')
  },
  addResizeHandler() {
    this.removeResizeHandler()
    $(window).on('resize.datePicker' + this.cid, this.hasOverflowed.bind(this))
  },
  removeResizeHandler() {
    $(window).off('resize.datePicker' + this.cid)
  },
  onDestroy() {
    this.removeResizeHandler()
  },
})
