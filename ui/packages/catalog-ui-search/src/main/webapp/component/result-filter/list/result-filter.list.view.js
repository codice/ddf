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

import React from 'react'
import ResultFilterView from '../../../react-component/container/result-filter'
import Marionette from 'marionette'
import CustomElements from '../../../js/CustomElements'

const closeDropdown = el => () =>
  el.trigger(`closeDropdown.${CustomElements.getNamespace()}`)

const removeFilter = (model, closeAction) => () => {
  model.set('value', '')
  closeAction()
}
const saveFilter = (model, closeAction) => transformedCql => {
  model.set('value', transformedCql)
  closeAction()
}

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <ResultFilterView
        removeFilter={removeFilter(this.model, closeDropdown(this.$el))}
        saveFilter={saveFilter(this.model, closeDropdown(this.$el))}
        hasFilter={!!this.model.get('value')}
        resultFilter={this.model.get('value')}
        isList={true}
      />
    )
  },
})
