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
import { connect } from 'react-redux'

import Searches from '../../presentation/searches'
import { getSearchesRequest } from './actions'

type Search = {
  id: string
  title: string
  owner: string
  created: string
  modified: string
}

type Props = {
  searches: Search[]
  getSearches: () => void
}

type State = {
  searches: Search[]
}

class SearchesContainer extends React.Component<Props, State> {
  componentDidMount() {
    this.props.getSearches()
  }

  render() {
    return <Searches searches={this.props.searches} />
  }
}

const mapStateToProps = (state: any) => ({
  searches: state.searchApp.searches,
})

const mapDispatchToProps = (dispatch: any) => ({
  getSearches: () => dispatch(getSearchesRequest()),
})

const Connected = connect(
  mapStateToProps,
  mapDispatchToProps
)(SearchesContainer)
export default hot(module)(Connected)
