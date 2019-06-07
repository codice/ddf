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
const React = require('react')

const CustomElements = require('../../../js/CustomElements.js')
const Component = CustomElements.registerReact('direction')

class Direction extends React.Component {
  getToggledOption() {
    return this.props.value === this.props.options[0]
      ? this.props.options[1]
      : this.props.options[0]
  }

  handleMouseDown(e) {
    e.preventDefault()
    this.props.onChange(this.getToggledOption())
  }

  handleKeyPress(e) {
    const toggledOption = this.getToggledOption()
    if (
      String.fromCharCode(e.which).toUpperCase() === toggledOption.toUpperCase()
    ) {
      this.props.onChange(toggledOption)
    }
  }

  render() {
    const { value } = this.props
    return (
      <Component>
        <input
          value={value}
          className="toggle-input"
          onMouseDown={this.handleMouseDown.bind(this)}
          onKeyPress={this.handleKeyPress.bind(this)}
          onChange={e => e.stopPropagation()}
        />
      </Component>
    )
  }
}

module.exports = Direction
