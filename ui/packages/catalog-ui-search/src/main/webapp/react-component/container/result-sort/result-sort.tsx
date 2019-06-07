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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import ResultSortPresentation from '../../presentation/result-sort'

const Backbone = require('backbone')
const user = require('../../../component/singletons/user-instance.js')

type Props = {
  closeDropdown: () => void
}

type State = {
  collection: Backbone.Collection<Backbone.Model>
}

export default hot(module)(
  class ResultSort extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props)
      const resultSort = user
        .get('user')
        .get('preferences')
        .get('resultSort')

      this.state = {
        collection: new Backbone.Collection(resultSort),
      }
    }

    removeSort = () => {
      user
        .get('user')
        .get('preferences')
        .set('resultSort', undefined)
      user
        .get('user')
        .get('preferences')
        .savePreferences()
    }

    saveSort = () => {
      const sorting = this.state.collection.toJSON()
      user
        .get('user')
        .get('preferences')
        .set('resultSort', sorting.length === 0 ? undefined : sorting)
      user
        .get('user')
        .get('preferences')
        .savePreferences()
    }

    render() {
      return (
        <ResultSortPresentation
          saveSort={this.saveSort}
          removeSort={this.removeSort}
          collection={this.state.collection}
          hasSort={this.state.collection && this.state.collection.length > 0}
        />
      )
    }
  }
)
