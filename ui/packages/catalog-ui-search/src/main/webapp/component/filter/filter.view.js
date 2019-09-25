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
import * as React from 'react'
import Filter from '../../react-component/filter'

module.exports = Marionette.LayoutView.extend({
  template() {
    const value = this.model.get('value')
    return (
      <Filter
        attribute={this.model.get('type')}
        comparator={this.model.get('comparator')}
        value={value ? value[0] : undefined}
        model={this.model}
        isValid={this.model.get('isValid')}
        {...this.options}
        editing={this.$el.hasClass('is-editing')}
        onRemove={() => this.delete()}
        onChange={state => this.onChange(state)}
      />
    )
  },
  attributes() {
    return { 'data-id': this.model.cid }
  },
  delete() {
    this.model.destroy()
  },
  onChange(state) {
    const { attribute, comparator, value } = state
    this.model.set({
      type: attribute,
      comparator,
      value: [value],
    })
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
