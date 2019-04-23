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
import { hot } from 'react-hot-loader'

const Root = styled.div`
  width: 100%;
  height: auto;
  display: inline-block;
  cursor: pointer;
  text-align: left;
`

const Header = styled.div`
  max-width: 100%;
  font-weight: bolder;
  padding: 0px ${props => props.theme.minimumSpacing};
  height: ${props => props.theme.minimumLineSize};
  line-height: ${props => props.theme.minimumLineSize};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const Details = styled.div`
  max-width: 100%;
  opacity: ${props => props.theme.minimumOpacity};
  padding: 0px ${props => props.theme.minimumSpacing};
  line-height: ${props => props.theme.minimumLineSize};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const Footer = styled.div`
  height: ${props => props.theme.minimumButtonSize};
  text-align: right;
  display: flex;
  justify-content: flex-end;
`

interface Props {
  header: React.ReactNode
  details: React.ReactNode
  footer: React.ReactNode
  className?: string
  style?: React.CSSProperties
  onClick?: (e: React.MouseEvent) => void
}

const Card = (props: Props) => {
  const { header, details, footer, className, style, onClick } = props
  return (
    <Root className={className} style={style as any} onClick={onClick}>
      <Header>{header}</Header>
      <Details>{details}</Details>
      <Footer>{footer}</Footer>
    </Root>
  )
}

export default hot(module)(Card)
