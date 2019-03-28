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
import { hot } from 'react-hot-loader'
import * as React from 'react'
import { Button, buttonTypeEnum } from '../button'
import styled from '../../styles/styled-components'
import { transparentize, readableColor } from 'polished'
import plugin from 'plugins/multi-select-actions'

type Props = {
  handleExport: () => void
  isDisabled: boolean
}

const Root = styled.div`
  border-left: 1px solid
    ${props =>
      transparentize(0.9, readableColor(props.theme.backgroundContent))};
`

const MultiSelectButton = styled(Button)`
  min-width: calc(2.5 * ${props => props.theme.minimumButtonSize});
  padding: 0px 5px 0px 5px;
`

const disabledStyle = {
  cursor: 'not-allowed',
  opacity: 0.3,
}

const Export = (props: Props) => (
  <MultiSelectButton
    style={props.isDisabled ? disabledStyle : {}}
    buttonType={buttonTypeEnum.neutral}
    onClick={props.handleExport}
    title={
      props.isDisabled
        ? 'Select one or more results to export.'
        : 'Export selected result(s).'
    }
  >
    <span style={{ paddingRight: '5px' }} className="fa fa-share" />
    <span>Export</span>
  </MultiSelectButton>
)

const buttons = plugin([Export])

const render = (props: Props) => {
  return (
    <Root>
      {buttons.map((Component: any, i: number) => (
        <Component key={i} {...props} />
      ))}
    </Root>
  )
}

export default hot(module)(render)
