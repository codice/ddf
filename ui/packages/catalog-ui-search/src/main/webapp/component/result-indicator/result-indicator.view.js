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
const store = require('../../js/store.js')
import ResultIndicator from './result-indicator'

module.exports = Marionette.ItemView.extend({
  template({ colors }) {
    return <ResultIndicator colors={colors} />
  },
  className: 'customElement',
  initialize() {
    this.debouncedRender = _.debounce(function() {
      if (!this.isDestroyed) {
        this.render()
      }
    }, 200)
    this.calculateColors()
  },
  serializeData() {
    return {
      colors: this.colors,
    }
  },
  calculateColors() {
    const self = this
    self.colors = []
    const currentWorkspace = store.getCurrentWorkspace()
    if (currentWorkspace) {
      currentWorkspace.get('queries').forEach(query => {
        if (!self.isDestroyed && query.get('result')) {
          const results = query.get('result').get('results')
          for (let i = 0; i <= results.length - 1; i++) {
            if (
              results.models[i]
                .get('metacard')
                .get('properties')
                .get('id') ===
              self.model
                .get('metacard')
                .get('properties')
                .get('id')
            ) {
              self.colors.push(query.color)
              self.debouncedRender()
              break
            }
          }
        }
      })
    }
  },
})
