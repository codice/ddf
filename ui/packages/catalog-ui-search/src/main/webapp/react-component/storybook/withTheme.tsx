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
import styled, { ThemeProvider } from '../styles/styled-components'
import themes, { ColorMode, SpacingMode } from './themes'

import '../../styles/fonts.css'

const { select } = require('@connexta/ace/@storybook/addon-knobs')

type Story = () => any

const Table = styled.div`
  min-height: 100vh;
  min-width: 100vw;
  display: flex;
  flex-direction: column;
  font-size: 14pt;
  font-family: 'Open Sans', arial, sans-serif;
`

const TableColumn = styled.div`
  display: flex;
  flex: 1;
  flex-direction: row;
`

const TableData = styled.div<any>`
  padding: 20px;
  box-sizing: border-box;
  flex: 1;
  background: ${props => props.theme.backgroundContent};
`

const withTheme = (story: Story) => {
  const colors = select(
    'Color Scheme',
    {
      Dark: ['dark'],
      Light: ['light'],
      Sea: ['sea'],
      All: ['dark', 'light', 'sea'],
    },
    ['dark']
  ) as ColorMode[]

  const spacing = select(
    'Spacing',
    {
      Comfortable: ['comfortable'],
      Cozy: ['cozy'],
      Compact: ['compact'],
      All: ['comfortable', 'cozy', 'compact'],
    },
    ['comfortable']
  ) as SpacingMode[]

  const el = story()

  return (
    <Table>
      <style>{'body {margin: 0;}'}</style>
      {colors.map(c => {
        return (
          <TableColumn>
            {spacing.map(s => {
              const theme = themes({ colors: c, spacing: s })
              return (
                <ThemeProvider theme={theme}>
                  <TableData>{el}</TableData>
                </ThemeProvider>
              )
            })}
          </TableColumn>
        )
      })}
    </Table>
  )
}

export default withTheme
