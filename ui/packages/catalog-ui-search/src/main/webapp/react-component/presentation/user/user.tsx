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
  email: string
  signOut: () => void
}

export default hot(module)(({ username, email, signOut }: Props) => {
  return (
    <Root>
      <div className="user-info">
        <div className="info-username is-large-font is-bold">{username}</div>
        <div className="info-email is-medium-font">{email}</div>
      </div>
      <div className="is-divider" />
      <Button
        buttonType={buttonTypeEnum.negative}
        text="Sign Out"
        onClick={signOut}
      />
    </Root>
  )
})
