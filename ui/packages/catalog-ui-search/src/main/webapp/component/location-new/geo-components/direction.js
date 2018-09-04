const React = require('react');

const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('direction');

class Direction extends React.Component {
    getToggledOption() {
        return (this.props.value === this.props.options[0])
                ? this.props.options[1]
                : this.props.options[0];
    }

    handleMouseDown(e) {
        e.preventDefault();
        this.props.onChange(this.getToggledOption());
    }

    handleKeyPress(e) {
        const toggledOption = this.getToggledOption();
        if (String.fromCharCode(e.which).toUpperCase() === toggledOption.toUpperCase()) {
            this.props.onChange(toggledOption);
        }
    }

    render() {
        const { value } = this.props;
        return (
            <Component>
                <input
                    value={value}
                    className='toggle-input'
                    onMouseDown={this.handleMouseDown.bind(this)}
                    onKeyPress={this.handleKeyPress.bind(this)}
                    onChange={(e) => e.stopPropagation()}
                />
            </Component>
        );
    }
}

module.exports = Direction;