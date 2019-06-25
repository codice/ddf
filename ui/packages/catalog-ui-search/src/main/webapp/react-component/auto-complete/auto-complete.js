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

const _debounce = require('lodash/debounce')

const Dropdown = require('../dropdown')
const TextField = require('../text-field')
const { Menu, MenuItem } = require('../menu')

class AutoComplete extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      input: '',
      loading: false,
      suggestions: [],
      error: null,
    }
    const { debounce = 500 } = this.props
    this.fetchSuggestions = _debounce(
      this.fetchSuggestions.bind(this),
      debounce
    )
  }
  async fetchSuggestions() {
    const { input } = this.state
    const { suggester, minimumInputLength = 3 } = this.props

    if (!(input.length < minimumInputLength)) {
      try {
        const suggestions = await suggester(input)
        this.setState({ loading: false, suggestions })
      } catch (e) {
        this.setState({ loading: false, error: 'Endpoint unavailable' }, () => {
          if (typeof this.props.onError === 'function') {
            this.props.onError(e)
          }
        })
      }
    } else {
      this.setState({ loading: false })
    }
  }
  onChange(input) {
    this.setState(
      { input, loading: true, suggestions: [], error: null },
      this.fetchSuggestions
    )
  }
  render() {
    const { error, input, loading, suggestions } = this.state
    const { minimumInputLength } = this.props
    const placeholder =
      input.length < minimumInputLength
        ? `Please enter ${minimumInputLength} or more characters`
        : undefined

    return (
      <Dropdown label={this.props.value || this.props.placeholder}>
        <div style={{ padding: 5 }}>
          <TextField
            autoFocus
            value={input}
            placeholder={placeholder}
            onChange={input => this.onChange(input)}
          />
        </div>
        {loading ? <div style={{ padding: '0 5px' }}>Searching...</div> : null}
        <Menu
          value={this.props.value}
          onChange={option => this.props.onChange(option)}
        >
          {suggestions.map(option => (
            <MenuItem key={option.id} value={option}>
              {option.name}
            </MenuItem>
          ))}
        </Menu>
        {!loading &&
        input.length >= minimumInputLength &&
        suggestions.length === 0 ? (
          <div style={{ padding: '0 5px' }}>{error || 'No results found'}</div>
        ) : null}
      </Dropdown>
    )
  }
}

module.exports = AutoComplete
