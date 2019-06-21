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
import MultiSelectActionsPresentation from '../../presentation/multi-select-actions'
import withListenTo, { WithBackboneProps } from '../backbone-container'

type Props = {
  selectionInterface: any
} & WithBackboneProps

type State = {
  isDisabled: boolean
}

class MultiSelectActions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      isDisabled: true,
    }
  }

  updateDisabled = () => {
    this.setState({
      isDisabled:
        this.props.selectionInterface.getSelectedResults().length === 0,
    })
  }

  componentDidMount = () => {
    this.props.listenTo(
      this.props.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateDisabled
    )
  }

  render() {
    return <MultiSelectActionsPresentation isDisabled={this.state.isDisabled} />
  }
}

export default hot(module)(withListenTo(MultiSelectActions))
