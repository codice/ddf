import {Component} from 'react'

export default class extends Component {
  componentWillMount () {
    const {on, ...rest} = this.props
    delete rest.off // delete potentially passed prop
    if (typeof on === 'function') {
      on(rest)
    }
  }
  componentWillUnmount () {
    const {off, ...rest} = this.props
    delete rest.on // delete potentially passed prop
    if (typeof this.props.off === 'function') {
      off(rest)
    }
  }

  componentDidMount () {
    const {did, ...rest} = this.props
    delete rest.did // delete potentially passed prop
    if (typeof this.props.did === 'function') {
      did(rest)
    }
  }

  render () {
    if (this.props.children !== undefined) {
      return this.props.children
    }

    return null
  }
}

