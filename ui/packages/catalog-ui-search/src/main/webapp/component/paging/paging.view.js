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
        page={serverPageIndex}
        hasNextServerPage={currentQuery.hasNextServerPage()}
        hasPreviousServerPage={currentQuery.hasPreviousServerPage()}
        onClickNext={this.nextServerPage.bind(this)}
        onClickPrevious={this.previousServerPage.bind(this)}
      />
    )
  },
  initialize(options) {
    const safeRender = () => {
      setTimeout(() => {
        if (!this.isDestroyed) {
          this.render()
        }
      }, 100)
    }

    this.listenTo(this.getQuery(), 'change:serverPageIndex', safeRender)
    this.listenTo(this.getQuery(), 'change:totalHits', safeRender)
    this.listenTo(this.model, 'reset', safeRender)

    this.updateSelectionInterface = _debounce(
      this.updateSelectionInterface,
      200,
      { leading: true, trailing: true }
    )
  },
  updateSelectionInterface() {
    this.options.selectionInterface.setActiveSearchResults(
      this.model.reduce((results, result) => {
        results.push(result)
        if (result.duplicates) {
          results = results.concat(result.duplicates)
        }
        return results
      }, [])
    )
  },
  previousServerPage() {
    this.getQuery().getPreviousServerPage()
  },
  nextServerPage() {
    this.getQuery().getNextServerPage()
  },
  onRender() {
    this.updateSelectionInterface()
  },
  serializeData() {
    return {}
  },
  getQuery() {
    return this.options.selectionInterface.getCurrentQuery()
  },
})
