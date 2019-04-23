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
import * as React from 'react'
import UserComponent from '../../presentation/user'
const user = require('../../../component/singletons/user-instance.js')
const announcement = require('../../../component/announcement/index.jsx')
const logoutActions = require('../../../component/singletons/logout-actions.js')
const $ = require('jquery')

interface State {
  username: string
  isGuest: boolean
  isIdp: boolean
  email: string
  password: string
}

class UserContainer extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props)
    this.state = {
      username:
        user.isGuest() && !logoutActions.isIdp() ? '' : user.getUserName(),
      isGuest: user.isGuest(),
      isIdp: logoutActions.isIdp(),
      email: user.getEmail(),
      password: '',
    }
  }
  signIn() {
    $.ajax({
      type: 'POST',
      url: './internal/login',
      data: {
        username: this.state.username,
        password: this.state.password,
        prevurl: window.location.href,
      },
      async: false,
      // @ts-ignore
      customErrorHandling: true,
      error: function() {
        announcement.announce({
          title: 'Sign In Failed',
          message:
            'Please verify your credentials and attempt to sign in again.',
          type: 'error',
        })
      },
      success: function() {
        document.location.reload()
      },
    })
  }
  signOut() {
    //this function is only here to handle clearing basic auth credentials
    //if you aren't using basic auth, this shouldn't do anything
    $.ajax({
      type: 'GET',
      url: './internal/user',
      async: false,
      username: '1',
      password: '1',
    }).then(function() {
      // @ts-ignore
      window.location =
        '../../logout/?prevurl=' + encodeURI(window.location.pathname)
    })
  }
  handleUsernameChange(value: string) {
    this.setState({
      username: value,
    })
  }
  handlePasswordChange(value: string) {
    this.setState({
      password: value,
    })
  }
  render() {
    const { username, isGuest, isIdp, email, password } = this.state
    return (
      <UserComponent
        username={username}
        isGuest={isGuest}
        isIdp={isIdp}
        email={email}
        handlePasswordChange={this.handlePasswordChange.bind(this)}
        handleUsernameChange={this.handleUsernameChange.bind(this)}
        password={password}
        signIn={this.signIn.bind(this)}
        signOut={this.signOut.bind(this)}
      />
    )
  }
}

export default UserContainer
