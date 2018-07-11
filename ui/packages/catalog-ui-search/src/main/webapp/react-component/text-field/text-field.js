const React = require('react');

const Group = require('../group');

const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('text-field');

class TextField extends React.Component {
    constructor(props) {
        super(props);
        this.state = { value: props.value };
    }
    componentWillReceiveProps(props) {
        if (document.activeElement !== this.ref) {
            this.setState({ value: props.value });
        }
    }
    onChange(value) {
        const { parse, onChange } = this.props;

        this.setState({ value }, () => {
            if (typeof onChange !== 'function') return;

            if (typeof parse !== 'function') {
                return onChange(this.state.value);
            }

            let parsed = true,
                parsedValue = undefined;

            try {
                parsedValue = parse(this.state.value);
            } catch (e) {
                parsed = false;
            }

            if (parsed) {
                onChange(parsedValue);
            }
        });
    }
    render() {
        const { label, addon, value, type = 'text', parse, onChange, ...props } = this.props;
        return (
            <Component>
                <Group>
                    {label !== undefined ? (
                        <span className="input-group-addon">{label}&nbsp;</span>
                    ) : null}
                    <input
                        ref={(ref) => (this.ref = ref)}
                        value={this.state.value || ''}
                        type={type}
                        onChange={(e) => {
                            this.onChange(e.target.value);
                        }}
                        {...props}
                    />
                    {addon !== undefined ? (
                        <label className="input-group-addon">{addon}</label>
                    ) : null}
                </Group>
            </Component>
        );
    }
}

module.exports = TextField;
