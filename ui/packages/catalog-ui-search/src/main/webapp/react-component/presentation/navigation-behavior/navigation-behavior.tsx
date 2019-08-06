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
import styled from '../../styles/styled-components'
import { readableColor, transparentize } from 'polished'
import { hot } from 'react-hot-loader'
const $ = require('jquery')

type Props = {
  className?: string
  style?: React.CSSProperties
}
type State = {
  open: boolean
}

const Wrapper = styled<{}, 'div'>('div')`
  > *:not(.composed-menu):hover:not(button),
  > *.is-active:not(.composed-menu):not(button),
  .composed-menu > *:not(.composed-menu):hover:not(button),
  .composed-menu > *:not(.composed-menu).is-active:not(button) {
    background: ${props =>
      transparentize(0.9, readableColor(props.theme.background))};
  }
  > button:not(.composed-menu):hover,
  > button.is-active:not(.composed-menu),
  .composed-menu > button:not(.composed-menu):hover,
  .composed-menu > button:not(.composed-menu).is-active {
    position: relative;
  }
  > button:not(.composed-menu):hover::before,
  > button.is-active:not(.composed-menu)::before,
  .composed-menu > button:not(.composed-menu):hover::before,
  .composed-menu > button:not(.composed-menu).is-active::before {
    content: '';
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0px;
    left: 0px;
    background: ${props =>
      transparentize(0.9, readableColor(props.theme.background))};
  }
`

const expandComposedMenus = (menuItems: any): any => {
  let expandedItems = [] as any
  let expanded = false
  menuItems.forEach((element: any) => {
    if ($(element).hasClass('composed-menu')) {
      expanded = true
      expandedItems = expandedItems.concat(
        $(element)
          .children()
          .toArray()
      )
    } else {
      expandedItems.push(element)
    }
  })
  if (expanded === false) {
    return expandedItems
  } else {
    return expandComposedMenus(expandedItems)
  }
}

const handleArrowKey = (componentView: any, up: boolean) => {
  const menuItems = componentView.getMenuItems()
  const currentActive = menuItems.filter((element: any) =>
    $(element).hasClass('is-active')
  )[0]
  const potentialNext =
    menuItems[menuItems.indexOf(currentActive) + (up === true ? -1 : 1)]
  if (potentialNext !== undefined) {
    $(currentActive).removeClass('is-active')
    $(potentialNext)
      .addClass('is-active')
      .focus()
  } else if (menuItems.indexOf(currentActive) === 0) {
    $(currentActive).removeClass('is-active')
    $(menuItems[menuItems.length - 1])
      .addClass('is-active')
      .focus()
  } else {
    $(currentActive).removeClass('is-active')
    $(menuItems[0])
      .addClass('is-active')
      .focus()
  }
}

const findEnclosingMenuItem = (
  menuItems: any,
  element: any,
  rootNode: any
): any => {
  const matchingMenuItem = menuItems[menuItems.indexOf(element)]
  if (matchingMenuItem) {
    return matchingMenuItem
  } else if (element === rootNode) {
    return undefined
  } else {
    return findEnclosingMenuItem(menuItems, element.parentNode, rootNode)
  }
}

class Dropdown extends React.Component<Props, State> {
  state = {
    open: false,
  }
  wrapperRef = React.createRef() as React.RefObject<HTMLDivElement>
  focus = () => {
    const menuItems = this.getMenuItems()
    $(menuItems).removeClass('is-active')
    $(menuItems[0])
      .addClass('is-active')
      .focus()
  }
  getMenuItems = () => {
    return this.getAllPossibleMenuItems().filter(
      (element: any) => element.offsetParent !== null
    )
  }
  getAllPossibleMenuItems = () => {
    if (!this.wrapperRef.current) {
      return
    }
    let menuItems = this.wrapperRef.current.childNodes
    let fullMenuItems = expandComposedMenus(menuItems)
    return fullMenuItems
  }
  listenToKeydown = () => {
    if (this.wrapperRef.current) {
      this.wrapperRef.current.addEventListener('keydown', this.handleKeydown)
      this.wrapperRef.current.addEventListener(
        'mouseover',
        this.handleMouseEnter
      )
    }
  }
  handleMouseEnter = (e: any) => {
    const menuItems = this.getMenuItems()
    const currentActive = menuItems.filter((element: any) =>
      $(element).hasClass('is-active')
    )[0]
    const mouseOver = findEnclosingMenuItem(
      menuItems,
      e.target,
      this.wrapperRef.current
    )
    if (mouseOver) {
      $(currentActive).removeClass('is-active')
      $(mouseOver)
        .addClass('is-active')
        .focus()
    }
  }
  handleUpArrow = () => {
    handleArrowKey(this, true)
  }
  handleDownArrow = () => {
    handleArrowKey(this, false)
  }
  /*
        buttons take action on keydown for enter in browsers, try it for yourself
        https://www.w3schools.com/tags/tryit.asp?filename=tryhtml_button_test
    */
  handleKeydown = (event: KeyboardEvent) => {
    let code = event.keyCode
    if (event.charCode && code == 0) code = event.charCode
    switch (code) {
      case 38:
        // Key up.
        event.preventDefault()
        this.handleUpArrow()
        break
      case 40:
        // Key down.
        event.preventDefault()
        this.handleDownArrow()
        break
    }
  }
  listenToFocusIn() {
    if (this.wrapperRef.current) {
      this.wrapperRef.current.addEventListener('focus', this.focus)
    }
  }
  componentDidMount() {
    this.listenToKeydown()
    this.listenToFocusIn()
    setTimeout(this.focus, 30)
  }
  componentWillUnmount() {}
  render() {
    const { className, style } = this.props
    return (
      <Wrapper
        tabIndex={0}
        innerRef={this.wrapperRef as any}
        className={`composed-menu ${className ? className : ''}`}
        style={style as any}
      >
        {this.props.children}
      </Wrapper>
    )
  }
}

export default hot(module)(Dropdown)
