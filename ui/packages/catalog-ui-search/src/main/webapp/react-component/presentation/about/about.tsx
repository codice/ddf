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
import { CustomElement } from '../../styles/mixins'
import { ChangeBackground } from '../../styles/mixins'
import { hot } from 'react-hot-loader'

interface Props {
  branding: string
  product: string
  version: string
  commitHash: string
  commitDate: string
  isDirty: boolean
  date: string
}

const Root = styled.div`
  ${CustomElement} ${props =>
    ChangeBackground(props.theme.backgroundContent)}
  overflow: auto;
  padding: ${props => props.theme.minimumSpacing} 0px;

  .about-content {
    margin: auto;
    max-width: ${props => {
      return props.theme.screenBelow(props.theme.mediumScreenSize)
        ? '100%'
        : '1200px'
    }};
    padding: 0px
      ${props =>
        props.theme.screenBelow(props.theme.mediumScreenSize)
          ? '20px'
          : '100px'};
  }

  .content-version,
  .version-message {
    padding: ${props => props.theme.minimumSpacing};
  }
`

export default hot(module)((props: Props) => {
  return (
    <Root>
      <div className="about-content is-large-font">
        <div>
          <span className="is-bold">{props.branding}</span>
          <span> {props.product}</span>
        </div>
        <div className="is-divider" />
        <div className="content-version">
          <div>
            <div className="version-title">Version</div>
            <div className="version-message is-medium-font">
              {props.version}
            </div>
          </div>
          <div className="is-divider" />
          <div>
            <div className="version-title">Unique Identifier</div>
            <div className="version-message is-medium-font">
              {`${props.commitHash} ${props.isDirty ? 'with Changes' : ''}`}
            </div>
          </div>
          <div className="is-divider" />
          <div>
            <div className="version-title">Release Date</div>
            <div className="version-message is-medium-font">{props.date}</div>
          </div>
        </div>
      </div>
    </Root>
  )
})
