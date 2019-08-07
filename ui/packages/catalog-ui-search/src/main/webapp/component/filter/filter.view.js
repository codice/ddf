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
const CustomElements = require('../../js/CustomElements.js')
import * as React from 'react'
import Filter from '../../react-component/filter'

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <Filter
        model={this.model}
        {...this.options}
        editing={this.$el.hasClass('is-editing')}
        onRemove={() => this.delete()}
      />
    )
  },
  tagName: CustomElements.register('filter'),
  attributes() {
    return { 'data-id': this.model.cid }
  },
  delete() {
    this.model.destroy()
  },
  turnOnEditing() {
    if (this.$el.hasClass('is-editing')) return
    this.$el.addClass('is-editing')
    this.render()
  },
  turnOffEditing() {
    if (!this.$el.hasClass('is-editing')) return
    this.$el.removeClass('is-editing')
    this.render()
  },
})
