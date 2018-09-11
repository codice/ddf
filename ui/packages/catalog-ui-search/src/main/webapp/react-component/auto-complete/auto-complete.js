const React = require('react')

const _debounce = require('lodash/debounce')
import fetch from '../../react-component/utils/fetch'

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
    this.fetch = props.fetch || fetch
    const { debounce = 500 } = this.props
    this.fetchSuggestions = _debounce(
      this.fetchSuggestions.bind(this),
      debounce
    )
  }
  async fetchSuggestions() {
    const { input } = this.state
    const { url, minimumInputLength = 3 } = this.props

    if (!(input.length < minimumInputLength)) {
      try {
        const res = await this.fetch(`${url}?q=${input}`)
        let suggestions = await res.json()
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
        <div style={{ padding: '0 5px' }}>
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
