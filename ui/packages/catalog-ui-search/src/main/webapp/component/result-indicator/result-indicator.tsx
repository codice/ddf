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
import { hot } from 'react-hot-loader'
import styled from '../../react-component/styles/styled-components'

const Flex = styled.div`
  display: flex;
  flex-direction: column;
  width: ${props => props.theme.multiple(0.5, props.theme.minimumSpacing)};
  height: 100%;
`

const ColorBand = styled<{ bandColor: string }, 'div'>('div')`
  width: 100%;
  height: 100%;
  background: ${props => props.bandColor};
`

type Props = {
  colors: any[]
}

const render = ({ colors }: Props) => {
  return (
    <Flex>
      {colors.map(colorFunc => {
        const color = colorFunc()
        return <ColorBand key={color} bandColor={color} />
      })}
    </Flex>
  )
}

export default hot(module)(render)
