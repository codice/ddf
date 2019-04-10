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
