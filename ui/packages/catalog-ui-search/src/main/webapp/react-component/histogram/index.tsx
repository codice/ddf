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
import styled from 'styled-components'

const Empty = styled.div`
  text-align: center;
  padding: ${props => props.theme.largeSpacing};
  display: none;
`
const Attribute = styled.div`
  display: block;
  opacity: 1;
  transition: opacity ${props => props.theme.coreTransitionTime} linear;
  transform: translateX(0%);
`
const NoData = styled.div`
  text-align: center;
  padding: ${props => props.theme.largeSpacing};
  display: none;
`
const Container = styled.div`
  display: block;
  height: calc(100% - 135px);
  opacity: 1;
  transition: opacity ${props => props.theme.coreTransitionTime} linear;
  transform: translateX(0%);
`
const Warning = styled.span`
  color: ${props => props.theme.warningColor};
`

export const HistogramContainer = () => (
  <React.Fragment>
    <Empty className="histogram-empty">
      <h3>Please select a result set to display on the histogram.</h3>
    </Empty>
    <Attribute className="histogram-attribute" />
    <NoData className="histogram-no-matching-data">
      <h3>
        <Warning className="fa fa-exclamation-triangle" />
        Nothing in the current result set contains this attribute.
      </h3>
    </NoData>
    <Container className="histogram-container" />
  </React.Fragment>
)
