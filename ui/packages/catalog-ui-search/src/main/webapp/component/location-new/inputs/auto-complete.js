const React = require('react');

const _debounce = require('lodash/debounce');
const fetch = require('./fetch');

const Dropdown = require('./dropdown');
const TextField = require('./text-field');
const { Menu, MenuItem } = require('./menu');

class AutoComplete extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            input: '',
            loading: false,
            suggestions: []
        };
        this.fetch = props.fetch || fetch;
        const { debounce = 500 } = this.props;
        this.fetchSuggestions = _debounce(this.fetchSuggestions.bind(this), debounce);
    }
    async fetchSuggestions() {
        const { input } = this.state;
        const { url, minimumInputLength = 3 } = this.props;

        if (!(input.length < minimumInputLength)) {
            const res = await this.fetch(`${url}?q=${input}`);
            let suggestions = await res.json();

            if (typeof suggestions[0] === 'string') {
                suggestions = suggestions.map((suggestion) => {
                    const match = suggestion.match(/(.*)\((.*)\)/);
                    return { id: suggestion, name: suggestion };
                });
            }

            this.setState({ loading: false, suggestions });
        } else {
            this.setState({ loading: false });
        }
    }
    onChange(input) {
        this.setState({ input, loading: true, suggestions: [] }, this.fetchSuggestions);
    }
    render() {
        const { input, loading, suggestions } = this.state;
        const { minimumInputLength } = this.props;
        const placeholder =
            input.length < minimumInputLength
                ? `Please enter ${minimumInputLength} or more characters`
                : undefined;

        return (
            <Dropdown label={this.props.value || this.props.placeholder}>
                <div style={{ padding: '0 5px' }}>
                    <TextField
                        autoFocus
                        value={input}
                        placeholder={placeholder}
                        onChange={(input) => this.onChange(input)}
                    />
                </div>
                {loading ? <div style={{ padding: '0 5px' }}>Searching...</div> : null}
                <Menu value={this.props.value} onChange={(option) => this.props.onChange(option)}>
                    {suggestions.map((option) => (
                        <MenuItem key={option.id} value={option}>
                            {option.name}
                        </MenuItem>
                    ))}
                </Menu>
                {!loading && input.length >= minimumInputLength && suggestions.length === 0 ? (
                    <div style={{ padding: '0 5px' }}>No results found</div>
                ) : null}
            </Dropdown>
        );
    }
}

module.exports = AutoComplete;
