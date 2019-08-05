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
import { useEffect } from 'react'
import styled from 'styled-components'
import { readableColor, rgba } from 'polished'

const mod = (n: any, m: any) => ((n % m) + m) % m

const MenuRoot = styled.div`
  max-height: 25vh;
  position: relative;
`

const after = `
  ::after {
    display: inline-block;
    content: '\f00c';
    font-family: FontAwesome;
    font-style: normal;
    position: absolute;
    top: 50%;
    right: 0px;
    width: 2.275rem;
    text-align: center;
    transform: translateY(-50%);
  }
`

const background = (props: any) => {
  if (props.theme.backgroundDropdown !== undefined) {
    return rgba(readableColor(props.theme.backgroundDropdown), 0.1)
  }
}

const foreground = (props: any) => {
  if (props.theme.backgroundDropdown !== undefined) {
    return readableColor(props.theme.backgroundDropdown)
  }
}

const ItemRoot = styled.div<{ active: boolean; selected: boolean }>`
  position: relative;
  padding: 0px ${({ theme }) => theme.minimumSpacing};
  padding-right: ${({ theme }) => theme.minimumButtonSize};
  box-sizing: border-box;
  height: ${({ theme }) => theme.minimumButtonSize};
  line-height: ${({ theme }) => theme.minimumButtonSize};
  cursor: pointer;
  -webkit-touch-callout: none; /* iOS Safari */
  -webkit-user-select: none; /* Safari */
  -khtml-user-select: none; /* Konqueror HTML */
  -moz-user-select: none; /* Firefox */
  -ms-user-select: none; /* Internet Explorer/Edge */
  user-select: none; /* Non-prefixed version, currently supported by Chrome and Opera */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  ${({ theme, active }) =>
    active ? `box-shadow: inset 0px 0px 0px 1px  ${theme.primaryColor};` : ''}
  ${({ selected }) => (selected ? 'font-weight: bold;' : '')}
  ${({ selected }) => (selected ? after : '')}
  background: ${props => (props.active ? background(props) : 'inherit')};
  color: ${foreground};
`

const DocumentListener = (props: any) => {
  useEffect(() => {
    document.addEventListener(props.event, props.listener)
    return () => {
      document.removeEventListener(props.event, props.listener)
    }
  }, [])
  return null
}

interface MenuProps {
  /** Currently selected value of the provided `<MenuItems />`. */
  value?: any
  /**
   * Determines if multiple items can be selected
   *
   * @default false
   */
  multi?: boolean
  /**
   * MenuItems
   */
  children?: any
  /** Optional value change handler. */
  onChange: (value: any) => void
  /** Optional className to style root menu element.  */
  className?: string

  onClose?: () => void
}

type MenuState = {
  active: boolean
}

export class Menu extends React.Component<MenuProps, MenuState> {
  constructor(props: MenuProps) {
    super(props)
    this.state = { active: this.chooseActive() }
    this.onKeyDown = this.onKeyDown.bind(this)
  }
  chooseActive() {
    const selection = this.props.value
    const active = this.state ? this.state.active : undefined
    const itemNames = this.getChildren().map((child: any) => child.props.value)
    if (itemNames.includes(active)) {
      return active
    } else if (itemNames.includes(selection)) {
      return selection
    } else if (itemNames.length > 0) {
      return itemNames[0]
    } else {
      return undefined
    }
  }
  onHover(active: any) {
    this.setState({ active })
  }
  getChildren() {
    return this.getChildrenFrom(this.props.children)
  }
  getChildrenFrom(children: any) {
    return React.Children.toArray(children).filter((o: any) => o)
  }
  onShift(offset: any) {
    const values = this.getChildren().map(({ props }: any) => props.value)
    const index = values.findIndex((value: any) => value === this.state.active)
    const next = mod(index + offset, values.length)
    this.onHover(values[next])
  }
  getValue(value: any) {
    if (this.props.multi) {
      if (this.props.value.indexOf(value) !== -1) {
        return this.props.value.filter((v: any) => v !== value)
      } else {
        return this.props.value.concat(value)
      }
    } else {
      return value
    }
  }
  onChange(value: any) {
    this.props.onChange(this.getValue(value))

    if (!this.props.multi && typeof this.props.onClose === 'function') {
      this.props.onClose()
    }
  }
  onKeyDown(e: any) {
    switch (e.code) {
      case 'ArrowUp':
        e.preventDefault()
        this.onShift(-1)
        break
      case 'ArrowDown':
        e.preventDefault()
        this.onShift(1)
        break
      case 'Enter':
        e.preventDefault()
        const { active } = this.state
        if (active !== undefined) {
          this.onChange(active)
        }
        break
    }
  }
  componentDidUpdate(previousProps: any) {
    if (previousProps.children !== this.props.children) {
      this.setState({ active: this.chooseActive() })
    }
  }
  render() {
    const { multi, value, children } = this.props

    const childrenWithProps = this.getChildrenFrom(children).map(
      (child: any) => {
        return React.cloneElement(child, {
          selected: multi
            ? value.indexOf(child.props.value) !== -1
            : value === child.props.value,
          onClick: () => this.onChange(child.props.value),
          active: this.state.active === child.props.value,
          onHover: () => this.onHover(child.props.value),
          ...child.props,
        })
      }
    )

    return (
      <MenuRoot className={this.props.className}>
        {childrenWithProps}
        <DocumentListener event="keydown" listener={this.onKeyDown} />
      </MenuRoot>
    )
  }
}

type MenuItemProps = {
  /** A value to represent the current Item */
  value?: any
  /**
   * Children to display for menu item.
   *
   * @default value
   */
  children?: any
  /** Optional styles for root element. */
  style?: object
  onClick?: any
  selected?: any
  active?: any
  onHover?: any
}

export const MenuItem = (props: MenuItemProps) => {
  const { value, children, selected, onClick, active, onHover, style } = props

  return (
    <ItemRoot
      selected={selected}
      active={active}
      style={style}
      onMouseEnter={() => onHover(value)}
      onFocus={() => onHover(value)}
      tabIndex={0}
      onClick={() => onClick(value)}
    >
      {children || value}
    </ItemRoot>
  )
}

// @ts-ignore
MenuItem.displayName = 'MenuItem'
