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
import SortSelections from '../../react-component/query-sort-selection/sort-selections'
const Marionette = require('marionette')

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <React.Fragment>
        <label
          style={{
            textAlign: 'left',
            padding: '0 1.5rem',
            fontWeight: 'bolder',
          }}
        >
          Sort
        </label>
        <SortSelections
          collection={this.collection}
          showBestTextOption={this.options.showBestTextOption}
        />
      </React.Fragment>
    )
  },
})
