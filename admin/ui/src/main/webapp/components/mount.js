import { Component } from 'react'

export default class extends Component {
  componentWillMount () {
    if (typeof this.props.on === 'function') {
      this.props.on()
    }
  }
  componentWillUnmount () {
    if (typeof this.props.off === 'function') {
      this.props.off()
    }
  }
  render () {
    return this.props.children
  }
}
