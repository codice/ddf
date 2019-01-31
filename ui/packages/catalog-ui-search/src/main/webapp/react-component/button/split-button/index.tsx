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
import { hot } from 'react-hot-loader'

type Props = {
  children: {
    label: any
    menu: any
  }
  style: object
  title: string
  className: string
  onSelect: () => void
}

type State = {
  isOpen: boolean
}

const Root = styled.div`
  position: relative;
  overflow: none;
  padding: 0;
  margin: 0;
  display: flex;
`
const Menu = styled.div`
  position: absolute;
  top: ${props => props.theme.minimumButtonSize};
  padding: 0;
  margin: 0;
  width: 100%;
  overflow: auto;
  z-index: ${props => props.theme.zIndexDropdown};
  background-color: ${props => props.theme.backgroundDropdown};
`

const MainButton = styled.button`
  flex: 1;
`

const Icon = styled.button`
  color: white;
  display: inline-block;
  background: ${props => props.theme.primaryColor};
  width: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  text-align: center;
  font-size: ${props => props.theme.largeFontSize};
  margin-left: ${props => props.theme.minimumSpacing};
`

class SplitButton extends React.Component<Props, State> {
  public static defaultProps = {
    title: '',
    style: {},
  }
  constructor(props: Props) {
    super(props)
    this.state = {
      isOpen: false,
    }
  }
  onToggle() {
    this.setState({ isOpen: !this.state.isOpen })
  }
  renderMenu(contents: Element) {
    return (
      <Menu onMouseUp={() => this.setState({ isOpen: false })}>{contents}</Menu>
    )
  }
  render() {
    const { label, menu } = this.props.children
    const { style = {}, title = '', onSelect, className = '' } = this.props
    return (
      <Root style={style} className={className}>
        <MainButton title={title} type="button" onClick={onSelect}>
          {label}
        </MainButton>
        <Icon className="fa fa-caret-down" onClick={() => this.onToggle()} />
        {this.state.isOpen ? this.renderMenu(menu) : null}
      </Root>
    )
  }
}

export default hot(module)(SplitButton)
