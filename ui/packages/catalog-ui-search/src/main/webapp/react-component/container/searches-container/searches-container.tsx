/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import * as React from 'react'
import { hot } from 'react-hot-loader'
import Searches from '../../presentation/searches'
import { createStore } from 'redux'
import searchApp from './reducers'
import { addSearch } from './actions';

var Provider = require('react-redux').Provider

type Search = {
  id: string
  title: string
  owner: string
  created: string
  modified: string
}

type State = {
  searches: Search[]
}

const store = createStore(searchApp)

const search = {
  id: 'metacardId',
  title: 'Search Title',
  owner: 'christopher.clark.bell@protonmail.com',
  created: 'April 03, 2019',
  modified: 'April 03, 2019'
}

class SearchesContainer extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props)
    this.state = {
      searches: [],
    }

    store.dispatch(addSearch(search))

    store.subscribe(() => {
      this.setState({
        searches: store.getState().searches
      })
    })
  }

  componentDidMount() {
    store.dispatch(addSearch(search))
    store.dispatch(addSearch(search))
    store.dispatch(addSearch(search))
    store.dispatch(addSearch(search))
    store.dispatch(addSearch(search))
  }

  render() {
    return (
      <Provider store={store}>
        <Searches searches={this.state.searches} />
      </Provider>
    )
  }
}

export default hot(module)(SearchesContainer)
