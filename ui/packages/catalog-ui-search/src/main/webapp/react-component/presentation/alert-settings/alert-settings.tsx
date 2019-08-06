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
import Enum from '../../container/input-wrappers/enum'
import { hot } from 'react-hot-loader'

type Value = {
  [key: string]: unknown
}

type Props = {
  persistence: boolean
  expiration: number
  onExpirationChange: (v: Value) => any
  onPersistenceChange: (v: Value) => any
}

const Root = styled<{}, 'div'>('div')`
  width: 100%;
  height: 100%;
  overflow: auto;
  font-size: ${props => props.theme.minimumFontSize};
  padding: ${props => props.theme.minimumSpacing};

  .editor-properties {
    overflow: auto;
  }
`

const render = (props: Props) => {
  const {
    persistence,
    expiration,
    onExpirationChange,
    onPersistenceChange,
  } = props
  const millisecondsInDay = 24 * 60 * 60 * 1000
  return (
    <Root>
      <div className="editor-properties">
        <Enum
          label="Keep notifications after logging out"
          options={[
            {
              label: 'Yes',
              value: true,
            },
            {
              label: 'No',
              value: false,
            },
          ]}
          value={persistence}
          onChange={onPersistenceChange}
        />
        {persistence ? (
          <Enum
            className="property-expiration"
            label="Expire after"
            options={[
              {
                label: '1 Day',
                value: millisecondsInDay,
              },
              {
                label: '2 Days',
                value: 2 * millisecondsInDay,
              },
              {
                label: '4 Days',
                value: 4 * millisecondsInDay,
              },
              {
                label: '1 Week',
                value: 7 * millisecondsInDay,
              },
              {
                label: '2 Weeks',
                value: 14 * millisecondsInDay,
              },
              {
                label: '1 Month',
                value: 30 * millisecondsInDay,
              },
              {
                label: '2 Months',
                value: 60 * millisecondsInDay,
              },
              {
                label: '4 Months',
                value: 120 * millisecondsInDay,
              },
              {
                label: '6 Months',
                value: 180 * millisecondsInDay,
              },
              {
                label: '1 Year',
                value: 365 * millisecondsInDay,
              },
            ]}
            value={expiration}
            onChange={onExpirationChange}
          />
        ) : null}
      </div>
    </Root>
  )
}

export default hot(module)(render)
