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
import React from 'react'
import PropTypes from 'prop-types'

import styled from '../styles/styled-components'
import { readableColor, rgba } from 'polished'

const foreground = props => {
  if (props.theme.backgroundDropdown !== undefined) {
    return readableColor(props.theme.backgroundDropdown)
  }
}

const background = (props, alpha = 0.4) => {
  if (props.theme.backgroundDropdown !== undefined) {
    return rgba(readableColor(props.theme.backgroundDropdown), alpha)
  }
}

const Root = styled.div`
  border-radius: ${props => props.theme.borderRadius};
  white-space: nowrap;
  background-color: inherit;
  border: 1px solid ${background};
  display: inline-block;
`

const Button = styled.button`
  vertical-align: top;
  opacity: ${props => props.theme.minimumOpacity};
  min-width: ${props => props.theme.minimumButtonSize};
  min-height: ${props => props.theme.minimumButtonSize};
  border: none;
  border-left: ${props =>
    !props.first ? '1px solid ' + background(props) : 'none'};
  background-color: inherit;
  padding: 0px 10px;
  box-size: border-box;
  cursor: pointer;
  font-size: ${props => props.theme.minimumFontSize};
  color: ${foreground};
  ${props =>
    props.selected
      ? `
    opacity: 1;
    font-weight: bolder;
    background: ${background(props, 0.1)};
  `
      : ''};
`

const Radio = props => {
  const { value, children, onChange } = props

  const childrenWithProps = React.Children.map(children, (child, i) => {
    return React.cloneElement(child, {
      first: i === 0,
      selected: value === child.props.value,
      onClick: () => onChange(child.props.value),
    })
  })

  return <Root>{childrenWithProps}</Root>
}

Radio.propTypes = {
  /** The currently selected RadioItem value. */
  value: PropTypes.string,
  /** Value change handler. */
  onChange: PropTypes.func,
  /** Instances of <RadioItem />. */
  children: PropTypes.node,
}

const RadioItem = props => {
  const { value, first, children, selected, onClick } = props
  return (
    <Button first={first} selected={selected} onClick={() => onClick(value)}>
      {children || value}
    </Button>
  )
}

RadioItem.propTypes = {
  /** Value to identity the item. */
  value: PropTypes.string,
}

export { Radio, RadioItem }
