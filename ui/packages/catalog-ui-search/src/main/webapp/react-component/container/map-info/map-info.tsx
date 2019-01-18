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
// import styled from '../../styles/styled-components'
import MapInfoPresentation from '../../presentation/map-info'
import { hot } from 'react-hot-loader'

const exampleLat = '14.94',
  exampleLon = '-11.875'
const examples: { [index: string]: string } = {
  decimal: `${exampleLat} ${exampleLon}`,
  mgrs: '4Q FL 23009 12331',
  utm: '14 1925mE 1513mN',
}



// const Span = styled.span`
//   padding-right: 5px;
// `
type State = {
  selected: string
}

class MapInfo extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props)
    this.state = {
      selected: `34°26′56″N 083°42′15″W`
    }
  }

  update(newFormat: string) {
    this.setState({ selected: newFormat })
  }

  render() {
    const { selected } = this.state

    const example = examples[selected]
    if (typeof example === 'undefined') {
      console.warn(`Unrecognized coordinate format value [${selected}]`)
    }

    const mapSettingsProps = {
      selected
    }

    return (<MapInfoPresentation {...mapSettingsProps}/>)
  }
}

export default hot(module)(MapInfo)
