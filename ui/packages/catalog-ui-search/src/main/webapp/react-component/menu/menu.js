const React = require('react')

const CustomElements = require('../../js/CustomElements.js')
const Component = CustomElements.registerReact('menu')

const mod = (n, m) => ((n % m) + m) % m

class DocumentListener extends React.Component {
  componentDidMount() {
    document.addEventListener(this.props.event, this.props.listener)
  }
  componentWillUnmount() {
    document.removeEventListener(this.props.event, this.props.listener)
  }
  render() {
    return null
  }
}

class Menu extends React.Component {
  constructor(props) {
    super(props)
    this.state = { active: this.chooseActive() }
    this.onKeyDown = this.onKeyDown.bind(this)
  }
  chooseActive() {
    const selection = this.props.value
    const active = this.state ? this.state.active : undefined
    const itemNames = React.Children.toArray(this.props.children).map(
      child => child.props.value
    )
    if (itemNames.includes(active)) {
      return active
    } else if (itemNames.includes(selection)) {
      return selection
    } else if (itemNames.length > 0) {
      return itemNames[0]
    } else {
      return undefined
    }
  }
  onHover(active) {
    this.setState({ active })
  }
  onShift(offset) {
    const values = React.Children.map(
      this.props.children,
      ({ props }) => props.value
    )
    const index = values.findIndex(value => value === this.state.active)
    const next = mod(index + offset, values.length)
    this.onHover(values[next])
  }
  getValue(value) {
    if (this.props.multi) {
      if (this.props.value.indexOf(value) !== -1) {
        return this.props.value.filter(v => v !== value)
      } else {
        return this.props.value.concat(value)
      }
    } else {
      return value
    }
  }
  onChange(value) {
    this.props.onChange(this.getValue(value))

    if (!this.props.multi && typeof this.props.onClose === 'function') {
      this.props.onClose()
    }
  }
  onKeyDown(e) {
    switch (e.code) {
      case 'ArrowUp':
        e.preventDefault()
        this.onShift(-1)
        break
      case 'ArrowDown':
        e.preventDefault()
        this.onShift(1)
        break
      case 'Enter':
        e.preventDefault()
        const { active } = this.state
        if (active !== undefined) {
          this.onChange(active)
        }
        break
    }
  }
  componentDidUpdate(previousProps) {
    if (previousProps.children !== this.props.children) {
      this.setState({ active: this.chooseActive() })
    }
  }
  render() {
    const { multi, value, children } = this.props

    const childrenWithProps = React.Children.map(children, (child, i) => {
      return React.cloneElement(child, {
        selected: multi
          ? value.indexOf(child.props.value) !== -1
          : value === child.props.value,
        onClick: () => this.onChange(child.props.value),
        active: this.state.active === child.props.value,
        onHover: () => this.onHover(child.props.value),
        ...child.props,
      })
    })

    return (
      <Component>
        {childrenWithProps}
        <DocumentListener event="keydown" listener={this.onKeyDown} />
      </Component>
    )
  }
}

class MenuItem extends React.Component {
  render() {
    const {
      value,
      children,
      selected,
      onClick,
      active,
      onHover,
      style,
    } = this.props
    return (
      <div
        style={style}
        onMouseEnter={() => onHover(value)}
        onFocus={() => onHover(value)}
        tabIndex="0"
        className={
          'input-menu-item ' +
          (selected ? ' is-selected ' : '') +
          (active ? ' is-active ' : '')
        }
        onClick={() => onClick(value)}
      >
        {children || value}
      </div>
    )
  }
}

exports.Menu = Menu
exports.MenuItem = MenuItem
