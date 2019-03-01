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
    <React.Fragment key="example.view.0">
      {' '}
      {/* surround with multiple child roots with this to avoid wrapper divs */}
      <div className="class1" /> {/* use className instead of class */}
      <ul>{listItems}</ul>
    </React.Fragment>
  )
}

const Template = props => {
  return (
    <React.Fragment key="example.view.1">
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
