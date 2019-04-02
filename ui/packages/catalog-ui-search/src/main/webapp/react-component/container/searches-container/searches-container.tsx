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
import fetch from '../../utils/fetch'

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

class SearchesContainer extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props)
    this.state = {
      searches: [],
    }
  }

  componentDidMount() {
    fetch('./internal/queries')
      .then(response => response.json())
      .then((searches: Search[]) => {
        this.setState({
          searches: searches,
        })
      })
  }

  render() {
    return <Searches searches={this.state.searches} />
  }
}

export default hot(module)(SearchesContainer)
