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
import TextField from '../../../react-component/container/input-wrappers/text'
import PasswordField from '../../../react-component/container/input-wrappers/password'
import { Button, buttonTypeEnum } from '../../presentation/button'
import { hot } from 'react-hot-loader'

const Root = styled<{}, 'div'>('div')`
  width: 100%;
  height: 100%;

  > button {
    width: 100%;
  }

  input {
    width: 100%;
  }

  .user-info {
    padding: ${props => props.theme.mediumSpacing};
    overflow: auto;
    max-height: calc(
      100% - ${props => props.theme.minimumButtonSize} -
        ${props => props.theme.minimumDividerSize}
    );
  }
`

type Props = {
  username: string
  password: string
  email: string
  isGuest: boolean
  isIdp: boolean
  signIn: () => void
  signOut: () => void
  handleUsernameChange: () => void
  handlePasswordChange: () => void
}

export default hot(module)(
  ({
    username,
    email,
    isGuest,
    isIdp,
    signIn,
    signOut,
    password,
    handleUsernameChange,
    handlePasswordChange,
  }: Props) => {
    return (
      <Root>
        {isGuest && !isIdp ? (
          <>
            <div className="user-info">
              <TextField
                placeholder="Username"
                value={username}
                showLabel={false}
                showValidationIssues={false}
                onChange={handleUsernameChange}
              />
              <PasswordField
                value={password}
                showLabel={false}
                showValidationIssues={false}
                onChange={handlePasswordChange}
                onKeyPress={event => {
                  if (event.key === 'Enter') {
                    signIn()
                  }
                }}
              />
            </div>
            <div className="is-divider" />
            <Button
              buttonType={buttonTypeEnum.primary}
              text="Sign In"
              onClick={signIn}
            />
          </>
        ) : (
          <>
            <div className="user-info">
              <div className="info-username is-large-font is-bold">
                {username}
              </div>
              <div className="info-email is-medium-font">{email}</div>
            </div>
            <div className="is-divider" />
            <Button
              buttonType={buttonTypeEnum.negative}
              text="Sign Out"
              onClick={signOut}
            />
          </>
        )}
      </Root>
    )
  }
)
