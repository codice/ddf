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
const CustomElements = require('../../../js/CustomElements.js')
const Marionette = require('marionette')
import React from 'react'

const ListItem = ({ label, value, children }) => {
  return (
    <li>
      {children}
      <div>{label}</div>
      <div>{value}</div>
    </li>
  )
}

const List = props => {
  const user = {
    name: 'Jim',
    password: '*******',
    id: 'Jim229',
    pet: 'cat',
  }
  const listItems = Object.keys(user)
    .filter(key => key !== 'password')
    .map((key, i) => {
      return (
        <ListItem
          key={
            key /* key is recommended, but not necessary since react doesn't control our rendering yet*/
          }
          label={key}
          value={user[key]}
        >
          <h1>{i}</h1>
        </ListItem>
      )
    })
  return (
    <React.Fragment>
      {' '}
      {/* surround with multiple child roots with this to avoid wrapper divs */}
      <div className="class1" /> {/* use className instead of class */}
      <ul>{listItems}</ul>
    </React.Fragment>
  )
}

const Template = props => {
  return (
    <React.Fragment>
      {' '}
      {/* surround with multiple child roots with this to avoid wrapper divs */}
      <h1> List of some Data</h1>
      <List {...props} />
    </React.Fragment>
  )
}

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('dev-jsx-guide-example'),
  template(props) {
    return <Template {...props} />
  },
})
