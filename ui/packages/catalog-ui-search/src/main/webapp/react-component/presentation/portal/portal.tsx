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
import * as ReactDOM from 'react-dom'
import { hot } from 'react-hot-loader'

class Portal extends React.Component<{}, {}> {
  wrapper = document.createElement('react-portal')
  constructor(props: {}) {
    super(props)
    document.body.appendChild(this.wrapper)
    this.setupWrapper()
  }
  /*
    Why this wrapper?  Well, styled-components doesn't have a good 
    abstraction for making a portal yet, so we keep the portal lightly styled 
    in a wrapper where we feed styled-components.  Hence the overflow set
    to visible.  This allows us to accomplish pretty much exactly what 
    we want.
  */
  setupWrapper() {
    this.wrapper.style.position = 'absolute'
    this.wrapper.style.left = '0px'
    this.wrapper.style.top = '0px'
    this.wrapper.style.display = 'block'
    this.wrapper.style.overflow = 'visible'
    this.wrapper.style.zIndex = '103' // use creation / append order from here on out
  }
  componentWillUnmount() {
    this.wrapper.remove()
  }
  render() {
    return ReactDOM.createPortal(this.props.children, this.wrapper)
  }
}

export default hot(module)(Portal)
