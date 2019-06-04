const React = require('react')

const Group = require('../../../react-component/group/index.js')
const MaskedInput = require('react-text-mask').default

const CustomElements = require('../../../js/CustomElements.js')
const Component = CustomElements.registerReact('text-field')
const { validateInput } = require('../utils/dms-utils')

class MaskedTextField extends React.Component {
  render() {
    const { label, addon, onChange, value = '', ...args } = this.props
    return (
      <Component>
        <Group>
          {label != null ? (
            <span className="input-group-addon">
              {label}
              &nbsp;
            </span>
          ) : null}
          <MaskedInput
            value={value}
            keepCharPositions
            onChange={e => {
              this.props.onChange(e.target.value)
            }}
            render={(setRef, { defaultValue, ...props }) => {
              return (
                <input
                  ref={ref => {
                    setRef(ref)
                    this.ref = ref
                  }}
                  value={defaultValue}
                  {...props}
                />
              )
            }}
            {...args}
          />
          {addon != null ? (
            <label className="input-group-addon">{addon}</label>
          ) : null}
        </Group>
      </Component>
    )
  }
}

module.exports = MaskedTextField
