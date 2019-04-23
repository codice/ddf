/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import { hot } from 'react-hot-loader'
import * as React from 'react'
import UserBlackListPresentation from '../../presentation/user-blacklist'
import withListenTo, { WithBackboneProps } from '../backbone-container'

const user = require('../../../component/singletons/user-instance.js')
type State = {
  blacklist: Backbone.Collection<Backbone.Model>
  clearing: boolean
}

class UserBlacklistContainer extends React.Component<WithBackboneProps, State> {
  constructor(props: WithBackboneProps) {
    super(props)
    this.state = {
      blacklist: this.getBlacklist(),
      clearing: false,
    }
  }

  componentDidMount = () => {
    this.props.listenTo(
      this.getBlacklist(),
      'add remove reset update',
      this.updateBlacklist
    )
  }

  updateBlacklist = () => {
    this.setState({
      blacklist: this.getBlacklist(),
    })
  }

  getBlacklist = () => {
    return user
      .get('user')
      .get('preferences')
      .get('resultBlacklist')
  }

  clearBlacklist = () => {
    this.setState({ clearing: true })
    setTimeout(() => {
      this.getBlacklist().reset()
      user.savePreferences()
      this.setState({ clearing: false })
    }, 250)
  }

  render() {
    return (
      <UserBlackListPresentation
        onClearBlacklist={this.clearBlacklist}
        blacklist={this.state.blacklist}
        clearing={this.state.clearing}
      />
    )
  }
}

export default hot(module)(withListenTo(UserBlacklistContainer))
