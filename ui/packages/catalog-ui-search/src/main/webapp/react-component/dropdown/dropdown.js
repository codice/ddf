const React = require('react')

const CustomElements = require('../../js/CustomElements.js')
const Component = CustomElements.registerReact('dropdown')

class Dropdown extends React.Component {
  constructor(props) {
    super(props)
    this.state = { open: false }
    this.onMouseDown = this.onMouseDown.bind(this)
    this.onKeyDown = this.onKeyDown.bind(this)
  }
  onMouseDown(e) {
    if (this.ref && !this.ref.contains(e.target)) {
      this.setState({ open: false })
    }
  }
  onKeyDown(e) {
    switch (e.code) {
      case 'Enter':
        e.preventDefault()
        if (document.activeElement === this.ref) {
          this.setState({ open: true })
        }
        break
    }
  }
  componentDidMount() {
    document.addEventListener('mousedown', this.onMouseDown)
    document.addEventListener('keydown', this.onKeyDown)
  }
  componentWillUnmount() {
    document.removeEventListener('mousedown', this.onMouseDown)
    document.removeEventListener('keydown', this.onKeyDown)
  }
  isOpen() {
    return this.props.open !== undefined ? this.props.open : this.state.open
  }
  render() {
    const { open } = this.state
    const { label, children } = this.props
    return (
      <Component
        className={`is-dropdown ${open ? 'is-open' : ''}`}
        tabIndex="0"
        ref={ref => (this.ref = ref)}
      >
        <div onClick={e => this.setState({ open: !open })}>
          <div className="dropdown-text is-input">
            <span className="text-src">
              <span>{label}</span>
            </span>
          </div>
          <span className="dropdown-icon fa fa-caret-down" />
        </div>
        {this.isOpen() ? (
          <div className="dropdown-area">
            {React.Children.map(children, child => {
              if (!React.isValidElement(child)) {
                return child
              }

              return React.cloneElement(child, {
                onClose: () => {
                  this.setState({ open: false })
                },
              })
            })}
          </div>
        ) : null}
      </Component>
    )
  }
}

module.exports = Dropdown
