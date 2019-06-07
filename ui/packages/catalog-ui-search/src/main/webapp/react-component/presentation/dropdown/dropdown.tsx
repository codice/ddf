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
import { hot } from 'react-hot-loader'
import Portal from '../portal'
import styled, { ThemeInterface } from '../../styles/styled-components'
import { Dropshadow } from '../../styles/mixins'
import ChangeBackground from '../change-background'
import ButtonBehavior from '../button-behavior'
import { SFC } from '../../hoc/utils'
import { CSSProperties } from 'react'
const CustomElements = require('../../../js/CustomElements.js')
const DropdownBehaviorUtility = require('../../../behaviors/dropdown.behavior.utility.js')
const $ = require('jquery')
const _ = require('underscore')

type Omit<T, K> = Pick<T, Exclude<keyof T, K>>
type Subtract<T, K> = Omit<T, keyof K>

export type Context = {
  /**
   * Close the dropdown
   */
  close: () => void
  /**
   * Close the dropdown and refocus on the element that opened it
   */
  closeAndRefocus: () => void
  parent: () => Context
  parentOpen: boolean
  /**
   * Close the dropdown and refocus on the element that opened it
   */
  deepCloseAndRefocus: () => void
  /**
   * Close the dropdown and refocus on the element that opened it
   */
  depthCloseAndRefocus: (depth: number) => void
}

export const DropdownContext = React.createContext({
  /**
   * Close the most immediate enclosing dropdown
   */
  close: () => {},
  /**
   * Close the most immediate enclosing dropdown
   * and refocus on the element that opened it
   */
  closeAndRefocus: () => {},
  /**
   * Returns a reference to the enclosing dropdown
   * Returns null if not within a dropdown
   */
  parent: (): Context => {
    return null as any
  },
  /**
   * Returns whether or not the enclosing dropdown is open
   */
  parentOpen: true,
  /**
   * Close all the enclosing dropdowns and refocus
   * on the element that opened the dropdowns
   */
  deepCloseAndRefocus: function(this: Context) {
    if (this.parent().parent() === null) {
      this.closeAndRefocus()
    } else {
      this.parent().deepCloseAndRefocus()
    }
  },
  /**
   * Close all the number of enclosing dropdowns
   * specified and refocus on the element that opened
   * the dropdown
   */
  depthCloseAndRefocus: function(this: Context, depth: number = 1) {
    if (this.parent().parent() === null || depth <= 1) {
      this.closeAndRefocus()
    } else {
      this.parent().depthCloseAndRefocus(depth - 1)
    }
  },
})

export type withContext = {
  dropdownContext: Context
}

type Props = {
  /**
   * content - What is displayed in the dropdown
   */
  content: JSX.Element | ((context: Context) => JSX.Element)
  contentClassName?: string
  contentStyle?: CSSProperties
  className?: string
  style?: CSSProperties
  theme?: ThemeInterface
} & withContext
type State = {
  open: boolean
  hasBeenOpen: boolean
}

export const withDropdown = <P extends withContext>(
  Component: React.ComponentType<P> | SFC<P>
) => {
  return function dropdownedComponent(props: Subtract<P, withContext>) {
    return (
      <DropdownContext.Consumer>
        {context => <Component {...props} dropdownContext={context} />}
      </DropdownContext.Consumer>
    )
  }
}

const DropdownWrapper = styled<{ open: boolean }, 'div'>('div')`
  display: block;
  position: absolute;
  z-index: ${props => props.theme.zIndexDropdown};
  overflow: auto;
  background: ${props => props.theme.background};
  padding-top: ${props => props.theme.minimumSpacing};
  padding-bottom: ${props => props.theme.minimumSpacing};
  border-radius: 2px;
  max-width: 90vw;
  max-height: 90vh;
  width: auto;
  height: auto;
  transition: opacity ${props => props.theme.coreTransitionTime} ease-in-out;
  opacity: ${props => (props.open ? 1 : 0)};
  ${props =>
    props.open
      ? `
  &.is-bottom {
    transform: translate3d(0, 0, 0) scale(1);
  }
  &.is-top {
    transform: translate3d(0, -100%, 0) scaleY(1);
  }
  `
      : `
  transform: translate3d(-50%, -50%, 0) scale(0);
  `};
  ${Dropshadow};
`

class Dropdown extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      open: false,
      hasBeenOpen: false,
    }
    // todo: remove after interop between legacy is no longer needed
    this.onClick = _.debounce(this.onClick, 10)
  }
  id = Math.random()
  dropdownRef = React.createRef() as React.RefObject<HTMLDivElement>
  sourceRef = React.createRef() as React.RefObject<HTMLButtonElement>
  componentWillUnmount() {
    $(window).off(`resize.${this.id}`)
    $('body').off(`mousedown.${this.id}`)
  }
  handleKeydown = (event: KeyboardEvent) => {
    let code = event.keyCode
    if (event.charCode && code == 0) code = event.charCode
    // Escape
    if (code === 27) {
      event.preventDefault()
      event.stopPropagation()
      this.close()
      this.refocusOnSource()
    }
  }
  listenForKeydown() {
    if (this.dropdownRef.current) {
      this.dropdownRef.current.addEventListener('keydown', this.handleKeydown)
    }
  }
  componentDidUpdate(_prevProps: Props, prevState: State) {
    this.updatePosition()
    this.handleOpen(prevState)
    this.handleHasBeenOpen(prevState)
    this.handleFirstOpen(prevState)
    this.handleParentOpen(_prevProps)
  }
  handleParentOpen(prevProps: Props) {
    if (
      prevProps.dropdownContext.parentOpen === true &&
      this.props.dropdownContext.parentOpen === false
    ) {
      this.setState({
        open: false,
      })
    }
  }
  handleFirstOpen(prevState: State) {
    if (prevState.hasBeenOpen === false && this.state.hasBeenOpen === true) {
      setTimeout(() => {
        this.updatePosition()
        $(window).on(`resize.${this.id}`, this.updatePosition)
        $('body').on(`mousedown.${this.id}`, this.handleOutsideInteraction)
        this.listenForKeydown()
        this.listenForClose()
        this.focus()
      }, 30)
    }
  }
  handleHasBeenOpen(prevState: State) {
    if (prevState.hasBeenOpen === false && this.state.open === true) {
      this.setState({
        hasBeenOpen: true,
      })
    }
  }
  handleOpen(prevState: State) {
    if (prevState.open === false && this.state.open === true) {
      this.focus()
    }
  }
  refocusOnSource() {
    if (this.sourceRef.current) {
      this.sourceRef.current.focus()
    }
  }
  /**
   * Attemps to focus on the first child, but if that fails at a minimum we'll be focused on the dropdown
   * which will allow them to tab to the first tabbable item.
   */
  focus() {
    if (this.dropdownRef.current) {
      this.dropdownRef.current.focus()
      const firstChild = this.dropdownRef.current.firstChild as HTMLElement
      if (firstChild) {
        firstChild.focus()
      }
    }
  }
  updatePosition = () => {
    if (this.dropdownRef.current && this.sourceRef.current) {
      DropdownBehaviorUtility.updatePosition(
        $(this.dropdownRef.current),
        this.sourceRef.current
      )
    }
  }
  withinDropdown = (element: HTMLDivElement) => {
    if (!this.sourceRef.current) {
      return false
    }
    return this.sourceRef.current.contains(element)
  }
  checkOutsideClick = (clickedElement: any) => {
    if (this.withinDropdown(clickedElement)) {
      return
    }
    if (
      DropdownBehaviorUtility.withinDOM(clickedElement) &&
      !DropdownBehaviorUtility.withinAnyDropdown(clickedElement)
    ) {
      this.close()
    }
    if (
      DropdownBehaviorUtility.withinParentDropdown(
        $(this.dropdownRef.current),
        clickedElement
      )
    ) {
      this.close()
    }
  }
  listenForClose() {
    if (this.dropdownRef.current) {
      $(this.dropdownRef.current).on(
        `closeDropdown.${CustomElements.getNamespace()}`,
        this.handleCloseDropdown
      )
    }
  }
  handleCloseDropdown = (e: Event) => {
    // stop from closing dropdowns higher in the dom
    e.stopPropagation()
    // close
    this.close()
    this.refocusOnSource()
  }
  closeAndRefocus = () => {
    this.close()
    this.refocusOnSource()
  }
  close = () => {
    this.setState({
      open: false,
    })
  }
  handleOutsideInteraction = (event: any) => {
    if (!DropdownBehaviorUtility.drawing(event)) {
      this.checkOutsideClick(event.target)
    }
  }
  onClick = () => {
    this.setState({
      open: !this.state.open,
    })
  }
  render() {
    const {
      className,
      style,
      contentClassName,
      contentStyle,
      dropdownContext,
    } = this.props
    const { open, hasBeenOpen } = this.state

    return (
      <>
        <ButtonBehavior
          ref={this.sourceRef as any}
          onClick={this.onClick}
          className={className}
          style={style}
        >
          {this.props.children}
        </ButtonBehavior>
        {hasBeenOpen ? (
          <DropdownContext.Provider
            value={{
              close: this.close,
              closeAndRefocus: this.closeAndRefocus,
              parent: () => dropdownContext,
              parentOpen: this.state.open,
              deepCloseAndRefocus: dropdownContext.deepCloseAndRefocus,
              depthCloseAndRefocus: dropdownContext.depthCloseAndRefocus,
            }}
          >
            <Portal>
              <ChangeBackground color={theme => theme.backgroundDropdown}>
                <DropdownWrapper
                  className={contentClassName}
                  innerRef={this.dropdownRef as any}
                  open={open}
                  tabIndex={0}
                  style={contentStyle as any}
                  data-react-dropdown
                >
                  {typeof this.props.content === 'function' ? (
                    <DropdownContext.Consumer>
                      {this.props.content}
                    </DropdownContext.Consumer>
                  ) : (
                    this.props.content
                  )}
                </DropdownWrapper>
              </ChangeBackground>
            </Portal>
          </DropdownContext.Provider>
        ) : null}
      </>
    )
  }
}

export default hot(module)(withDropdown(Dropdown))
