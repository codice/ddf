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
//import styled from '../../styles/styled-components'
import LayerItemPresentation from '../../presentation/layer-item'
//import Dropdown from '../../presentation/dropdown'
import { hot } from 'react-hot-loader'

// const Span = styled.span`
//   padding-right: 5px;
// `

type State = {
  selected: string
}

class LayerItem extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props)
    this.state = {
      selected: '',
    }
  }

  update = (a: string) => a

  render() {
    const { selected } = this.state

    const mapSettingsProps = {
      selected,
      update: (newFormat: string) => this.update(newFormat),
    }

    return <LayerItemPresentation {...mapSettingsProps} />
  }
}

export default hot(module)(LayerItem)
