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

const CustomElements = require('../../js/CustomElements.js')
const Component = CustomElements.registerReact('radio')

const Radio = props => {
  const { value, children, onChange } = props

  const childrenWithProps = React.Children.map(children, (child, i) => {
    return React.cloneElement(child, {
      selected: value === child.props.value,
      onClick: () => onChange(child.props.value),
    })
  })

  return <Component className="input-radio">{childrenWithProps}</Component>
}

const RadioItem = props => {
  const { value, children, selected, onClick } = props
  return (
    <button
      className={'input-radio-item ' + (selected ? 'is-selected' : '')}
      onClick={() => onClick(value)}
    >
      {children || value}
    </button>
  )
}

exports.Radio = Radio
exports.RadioItem = RadioItem
