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
import styled from '../../../react-component/styles/styled-components'
import {
  buttonTypeEnum,
  Button,
} from '../../../react-component/presentation/button'
import { hot } from 'react-hot-loader'

type Props = {
  value?: string
}

type State = {
  showPassword: boolean
}

const Root = styled<{}, 'div'>('div')`
  width: 100%;

  input {
    width: 100%;
    padding-right: ${props => props.theme.minimumButtonSize};
  }
  button {
    position: absolute;
    right: 0px;
    top: 0px;
  }
`

class Password extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      showPassword: false,
    }
  }
  toggleShowPassword = () => {
    this.setState({
      showPassword: !this.state.showPassword,
    })
  }
  render() {
    const { value } = this.props
    const { showPassword } = this.state
    return (
      <Root>
        <input
          placeholder="Password"
          value={value}
          type={showPassword ? 'text' : 'password'}
        />
        <Button
          buttonType={buttonTypeEnum.neutral}
          icon={`fa ${showPassword ? 'fa-eye-slash' : 'fa-eye'}`}
          onClick={this.toggleShowPassword}
          title={showPassword ? 'Hide Password' : 'Show Password'}
        />
      </Root>
    )
  }
}

export default hot(module)(Password)
