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
const $ = require('jquery')

interface Props {
  turnOffDrawing: Function
}

class CancelDrawingContainer extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props)
  }
  ref = React.createRef()
  componentDidMount() {
    $(this.ref.current).on('mousedown', (e: Event) => {
      e.stopPropagation()
      this.props.turnOffDrawing()
    })
  }
  render() {
    const children = React.Children.map(this.props.children, child => {
      return React.cloneElement(child as React.ReactElement<any>, {
        innerRef: this.ref,
      })
    })
    return children
  }
}

export default CancelDrawingContainer
