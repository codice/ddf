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
const isEqual = require('lodash/isEqual')

const CustomElements = require('../../js/CustomElements.js')
const Component = CustomElements.registerReact('json')

const Button = require('../button')

const getInitState = props => {
  const text = JSON.stringify(props.value, null, 2)
  const value = JSON.parse(text)
  return { value, text, error: null }
}

class Json extends React.Component {
  constructor(props) {
    super(props)
    this.state = getInitState(props)
  }
  componentWillReceiveProps(props) {
    this.setState(getInitState(props))
  }
  onChange(text) {
    this.setState({ text, error: null }, () => {
      try {
        const json = JSON.parse(text)
        if (!isEqual(this.state.value, json)) {
          this.props.onChange(json)
        }
      } catch (e) {
        this.setState({ error: e.message })
      }
    })
  }
  format() {
    const text = JSON.stringify(JSON.parse(this.state.text), null, 2)
    this.setState({ text })
  }
  render() {
    const { text, error } = this.state
    const { on = false } = this.props
    const lines = (text.match(/\n/g) || []).length + 1
    const readOnly = typeof this.props.onChange !== 'function'

    if (!on) return null

    return (
      <Component>
        {!readOnly ? (
          <Button disabled={error !== null} onClick={() => this.format()}>
            Format
          </Button>
        ) : null}
        <textarea
          rows={lines}
          value={text}
          readOnly={readOnly}
          onChange={e => this.onChange(e.target.value)}
        />
        {!readOnly ? <div className="error">{error}</div> : null}
      </Component>
    )
  }
}

module.exports = Json
