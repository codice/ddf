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

import * as React from 'react'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const _debounce = require('lodash/debounce')
import Paging from './paging'

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('paging'),
  template() {
    const currentQuery = this.getQuery()
    const serverPageIndex = currentQuery.get('serverPageIndex')
    return (
      <Paging
        page={serverPageIndex + 1}
        hasNextServerPage={currentQuery.hasNextServerPage()}
        hasPreviousServerPage={currentQuery.hasPreviousServerPage()}
        onClickNext={this.nextServerPage.bind(this)}
        onClickPrevious={this.previousServerPage.bind(this)}
      />
    )
  },
  initialize: function(options) {
    this.listenTo(this.model, 'reset', () => {
      setTimeout(() => {
        this.render
      }, 100)
    })
    this.listenTo(
      this.model,
      'add remove update',
      this.updateSelectionInterfaceComplete
    )
    this.updateSelectionInterface = _debounce(
      this.updateSelectionInterface,
      200,
      { leading: true, trailing: true }
    )
  },
  updateSelectionInterface: function() {
    this.options.selectionInterface.setActiveSearchResults(
      this.model.reduce(function(results, result) {
        results.push(result)
        if (result.duplicates) {
          results = results.concat(result.duplicates)
        }
        return results
      }, [])
    )
  },
  previousServerPage: function() {
    this.getQuery().getPreviousServerPage()
  },
  nextServerPage: function() {
    this.getQuery().getNextServerPage()
  },
  onRender: function() {
    this.updateSelectionInterface()
  },
  serializeData: function() {
    return {}
  },
  getQuery: function() {
    return this.options.selectionInterface.getCurrentQuery()
  },
})
