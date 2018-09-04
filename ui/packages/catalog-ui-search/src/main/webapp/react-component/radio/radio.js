const React = require('react');

const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('radio');

const Radio = (props) => {
    const { value, children, onChange } = props;

    const childrenWithProps = React.Children.map(children, (child, i) => {
        return React.cloneElement(child, {
            selected: value === child.props.value,
            onClick: () => onChange(child.props.value)
        });
    });

    return <Component className="input-radio">{childrenWithProps}</Component>;
};

const RadioItem = (props) => {
    const { value, children, selected, onClick } = props;
    return (
        <button
            className={'input-radio-item ' + (selected ? 'is-selected' : '')}
            onClick={() => onClick(value)}
        >
            {children || value}
        </button>
    );
};

exports.Radio = Radio;
exports.RadioItem = RadioItem;
