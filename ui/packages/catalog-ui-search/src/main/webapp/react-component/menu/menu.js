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
    const children = React.Children.toArray(props.children)
    this.state = {
      active: children.length > 0 ? children[0].props.value : undefined,
    }
    this.onKeyDown = this.onKeyDown.bind(this)
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
  onChange(value) {
    this.props.onChange(value)

    if (typeof this.props.onClose === 'function') {
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
  render() {
    const { value, children } = this.props

    const childrenWithProps = React.Children.map(children, (child, i) => {
      return React.cloneElement(child, {
        selected: value === child.props.value,
        onClick: () => this.onChange(child.props.value),
        active: this.state.active === child.props.value,
        onHover: () => this.onHover(child.props.value),
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
    const { value, children, selected, onClick, active, onHover } = this.props
    if (active && this.ref !== undefined) {
      this.ref.scrollIntoView({ block: 'nearest' })
    }
    return (
      <div
        ref={ref => (this.ref = ref)}
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
