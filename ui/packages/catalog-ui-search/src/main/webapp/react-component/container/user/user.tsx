/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import UserComponent from '../../presentation/user'
const user = require('../../../component/singletons/user-instance.js')

interface State {
  username: string
  email: string
}

class UserContainer extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props)
    this.state = {
      username: user.getUserName(),
      email: user.getEmail(),
    }
  }
  signOut() {
    window.location.href = '../../logout?service=' + window.location.href
  }
  render() {
    const { username, email } = this.state
    return (
      <UserComponent
        username={username}
        email={email}
        signOut={this.signOut.bind(this)}
      />
    )
  }
}

export default UserContainer
