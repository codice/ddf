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
import { createPortal } from 'react-dom'
import PropTypes from 'prop-types'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCaretDown } from '@fortawesome/free-solid-svg-icons/faCaretDown'
import isEqual from 'lodash/isEqual'
import styled from 'styled-components'
import { readableColor, rgba } from 'polished'

class Poller extends React.Component {
  constructor(props) {
    super(props)
    this.state = props.fn()
  }
  componentDidMount = () => {
    if (!this.id) {
      this.id = window.requestAnimationFrame(this.loop)
    }
  }
  componentWillUnmount = () => {
    window.cancelAnimationFrame(this.id)
  }
  loop = () => {
    const state = this.props.fn()
    if (!isEqual(state, this.state)) {
      this.setState(state)
    }
    this.id = window.requestAnimationFrame(this.loop)
  }
  render() {
    return React.Children.map(this.props.children, child => {
      if (!React.isValidElement(child)) {
        return child
      }

      return React.cloneElement(child, this.state)
    })
  }
}

const getPosition = (viewport, rect) => {
  const { x, y, width, height } = rect
  const top = y + height
  const bottom = viewport.height - y
  const pos = top > viewport.height / 2 ? { bottom } : { top }
  return {
    transform: `translateX(calc(-50% + ${width / 2}px))`,
    width: 'auto',
    minWidth: width,
    left: x,
    ...pos,
  }
}

const foreground = props => {
  if (props.theme.backgroundDropdown !== undefined) {
    return readableColor(props.theme.backgroundDropdown)
  }
}

const background = props => {
  if (props.theme.backgroundDropdown !== undefined) {
    return rgba(readableColor(props.theme.backgroundDropdown), 0.1)
  }
}

const Area = styled.div`
  position: fixed;
  z-index: ${props => props.theme.zIndexDropdown};

  overflow: auto;
  border-radius: ${props => props.theme.borderRadius};

  color: ${foreground};
  background-color: ${props => props.theme.backgroundDropdown};
  box-shadow: 0px 0px 2px 1px rgba(255, 255, 255, 0.4),
    2px 2px 10px 2px rgba(0, 0, 0, 0.4);
  max-width: 90vw;
  max-height: 50vh;
`

class DropdownArea extends React.Component {
  ref = React.createRef()
  stopPropagation = e => {
    e.stopPropagation()
  }
  componentDidMount() {
    if (this.ref) {
      this.ref.addEventListener('mousedown', this.stopPropagation)
      this.ref.addEventListener('mouseup', this.stopPropagation)
    }
  }
  componentWillUnmount() {
    if (this.ref) {
      this.ref.removeEventListener('mousedown', this.stopPropagation)
      this.ref.removeEventListener('mouseup', this.stopPropagation)
    }
  }
  render() {
    const { rect, children, onClose } = this.props
    const viewport = document.body.getBoundingClientRect()
    const style = getPosition(viewport, rect)
    return (
      <Area style={style} ref={ref => (this.ref = ref)}>
        {React.Children.map(children, child => {
          if (!React.isValidElement(child)) {
            return child
          }

          return React.cloneElement(child, {
            onClose,
          })
        })}
      </Area>
    )
  }
}

const Icon = styled.div`
  color: white;
  display: inline-block;
  color: #fff;
  background: ${props => props.theme.primaryColor};
  width: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  text-align: center;
  box-sizing: border-box;
  font-size: ${props => props.theme.largeFontSize};
  border-radius: ${props => props.theme.borderRadius};
`

const Text = styled.div`
  padding: 0px 10px;
  vertical-align: top;
  width: calc(100% - ${props => props.theme.minimumButtonSize});
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  display: block;
  display: inline-block;
  text-overflow: ellipsis;
  overflow: hidden;
  box-sizing: border-box;
  color: ${foreground};
  background: ${background};

  border: 2px solid ${background};
  outline: none;

  border-radius: ${props => props.theme.borderRadius};
`

const Component = styled.div`
  cursor: pointer;
  display: block;
  white-space: nowrap;
  position: relative;
  background: ${props => props.theme.backgroundDropdown};
`

class Dropdown extends React.Component {
  constructor(props) {
    super(props)
    this.state = { open: false, rect: null }
    this.onMouseDown = this.onMouseDown.bind(this)
    this.onKeyDown = this.onKeyDown.bind(this)
    this.el = document.createElement('div')
  }
  getAction = open => {
    if (open) {
      return this.props.onOpen
    } else {
      return this.props.onClose
    }
  }
  onToggle = next => {
    const open = next !== undefined ? next : !this.state.open
    this.setState({ open })
    const fn = this.getAction(open)
    if (typeof fn === 'function') {
      fn()
    }
  }
  onMouseDown(e) {
    if (this.state.open && this.ref && !this.ref.contains(e.target)) {
      this.onToggle(false)
    }
  }
  onKeyDown(e) {
    switch (e.code) {
      case 'Enter':
        e.preventDefault()
        if (document.activeElement === this.ref) {
          this.onToggle(true)
        }
        break
    }
  }
  getRect = () => {
    const { x, y, width, height } =
      this.ref !== undefined ? this.ref.getBoundingClientRect() : {}
    return { rect: { x, y, width, height } }
  }
  componentDidMount() {
    document.addEventListener('mousedown', this.onMouseDown)
    document.addEventListener('keydown', this.onKeyDown)
    document.body.appendChild(this.el)
  }
  componentWillUnmount() {
    document.removeEventListener('mousedown', this.onMouseDown)
    document.removeEventListener('keydown', this.onKeyDown)
    document.body.removeChild(this.el)
  }
  isOpen() {
    return this.props.open !== undefined ? this.props.open : this.state.open
  }
  render() {
    const anchor = this.props.anchor ? (
      React.cloneElement(this.props.anchor, { onClick: () => this.onToggle() })
    ) : (
      <div onClick={() => this.onToggle()}>
        <Text>{this.props.label}</Text>
        <Icon>
          <FontAwesomeIcon icon={faCaretDown} />
        </Icon>
      </div>
    )

    return (
      <Component>
        <div tabIndex="0" ref={ref => (this.ref = ref)}>
          {anchor}
          {this.isOpen() ? (
            <div>
              {createPortal(
                <Poller fn={this.getRect}>
                  <DropdownArea
                    onClose={() => {
                      this.onToggle(false)
                    }}
                  >
                    {this.props.children}
                  </DropdownArea>
                </Poller>,
                this.el
              )}
            </div>
          ) : null}
        </div>
      </Component>
    )
  }
}

Dropdown.propTypes = {
  /** Turn dropdown into a controlled component and force dropdown state. */
  open: PropTypes.bool,
  /** The label to use for the default anchor. */
  label: PropTypes.node,
  /** Component to anchor the dropdown. */
  anchor: PropTypes.element,
  /** Component to show when dropdown is open. */
  children: PropTypes.node,

  /** Open handler. */
  onOpen: PropTypes.func,
  /** Close handler. */
  onClose: PropTypes.func,
}

module.exports = Dropdown
