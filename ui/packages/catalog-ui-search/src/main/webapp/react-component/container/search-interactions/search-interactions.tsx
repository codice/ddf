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

import { deleteSearchRequest } from '../searches-container/actions'
import SearchInteractionsPresentation from '../../presentation/search-interactions'

type Props = {
  id: string
  deleteSearch: (id: string) => void
}

class SearchInteractions extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props)
  }

  render() {
    return (
      <SearchInteractionsPresentation
        id={this.props.id}
        editSearch={() => {}}
        deleteSearch={this.props.deleteSearch}
        runSearch={() => {}}
      />
    )
  }
}

const mapDispatchToProps = (dispatch: any) => ({
  deleteSearch: (id: string) => dispatch(deleteSearchRequest(id)),
})

const Connected = connect(
  null,
  mapDispatchToProps
)(SearchInteractions)
export default hot(module)(Connected)
