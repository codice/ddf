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

const Group = require('../../../react-component/group/index.js')
const CustomElements = require('../../../js/CustomElements.js')
const Component = CustomElements.registerReact('list-editor')

class ListEditor extends React.Component {
  handleAdd() {
    const { list, defaultItem, onChange } = this.props
    const newList = list.slice()
    newList.push(defaultItem)
    onChange(newList)
  }

  handleRemove(index) {
    const { list, onChange } = this.props
    const newList = list.slice()
    newList.splice(index, 1)
    onChange(newList)
  }

  render() {
    const listItems = React.Children.map(
      this.props.children,
      (child, index) => (
        <li className="item">
          <Group>
            {child}
            <button
              className="button-remove is-negative"
              onClick={this.handleRemove.bind(this, index)}
            >
              <span className="fa fa-minus" />
            </button>
          </Group>
        </li>
      )
    )
    return (
      <Component>
        <ul className="list">{listItems}</ul>
        <button
          className="button-add is-positive"
          onClick={this.handleAdd.bind(this)}
        >
          <span className="fa fa-plus" />
        </button>
      </Component>
    )
  }
}

module.exports = ListEditor
