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
import styled from '../../styles/styled-components'
const sessionTimeoutModel = require('../../../component/singletons/session-timeout')
import { Button, buttonTypeEnum } from '../button'

const SessionTimeoutRoot = styled.div`
  height: 100%;
  width: 100%;
  display: block;
  overflow: hidden;
`
const Message = styled.div`
  max-height: calc(100% - 2.25rem);
  height: auto;
  text-align: center;
  padding: ${props => props.theme.mediumSpacing};
`
const ButtonStyling = {
  height: '2.75rem',
  width: '100%',
}

type State = {
  timeLeft: number
}

const renewSession = () => {
  sessionTimeoutModel.renew()
}

class SessionTimeout extends React.Component<{}, State> {
  interval: any
  constructor(props: any) {
    super(props)
    this.state = {
      timeLeft: sessionTimeoutModel.getIdleSeconds(),
    }
  }
  componentDidMount() {
    this.interval = setInterval(
      () => this.setState({ timeLeft: sessionTimeoutModel.getIdleSeconds() }),
      1000
    )
  }
  componentWillUnmount() {
    clearInterval(this.interval)
  }
  render() {
    return this.state.timeLeft < 0 ? (
      <SessionTimeoutRoot>
        <Message>Session Expired. Please refresh the page to continue.</Message>
      </SessionTimeoutRoot>
    ) : (
      <SessionTimeoutRoot>
        <Message>
          You will be logged out automatically in{' '}
          <label className="timer">
            {sessionTimeoutModel.getIdleSeconds()}
          </label>{' '}
          seconds.
          <div>Press "Continue Working" to remain logged in.</div>
        </Message>
        <Button
          buttonType={buttonTypeEnum.primary}
          onClick={renewSession}
          style={ButtonStyling}
        >
          Continue Working
        </Button>
      </SessionTimeoutRoot>
    )
  }
}

export default SessionTimeout
