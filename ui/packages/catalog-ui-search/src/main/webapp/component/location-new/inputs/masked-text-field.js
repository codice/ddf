const React = require('react');

const Group = require('react-component/group');
const MaskedInput = require('react-text-mask').default;

const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('text-field');

const MaskedTextField = (props) => {
    const { label, addon, onChange, ...args } = props;
    return (
        <Component>
            <Group>
                {label != null ? (
                    <span className="input-group-addon">{label}&nbsp;</span>
                ) : null}
                <MaskedInput
                    onChange={(e) => onChange(e.target.value)}
                    {...args}
                />
                {addon != null ? (
                    <label className="input-group-addon">{addon}</label>
                ) : null}
            </Group>
        </Component>
    );
};

module.exports = MaskedTextField;
