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
import styled from '../../styles/styled-components'
import { CustomElement } from '../../styles/mixins'
const LoadingCompanionView = require('../../../component/loading-companion/loading-companion.view.js')

const Root = styled.div`
  ${CustomElement};
`

export interface Props {
  loading: boolean
}

export default class LoadingCompanionContainer extends React.Component<
  Props,
  {}
> {
  constructor(props: Props) {
    super(props)
  }
  ref = React.createRef()
  componentDidUpdate() {
    if (this.props.loading === false) {
      LoadingCompanionView.stopLoadingElement(this.ref.current)
    }
  }
  componentDidMount() {
    LoadingCompanionView.loadElement(this.ref.current)
  }
  componentWillUnmount() {
    LoadingCompanionView.stopLoadingElement(this.ref.current)
  }
  render() {
    return (
      <Root innerRef={this.ref as React.RefObject<HTMLDivElement>}>
        {this.props.children}
      </Root>
    )
  }
}
