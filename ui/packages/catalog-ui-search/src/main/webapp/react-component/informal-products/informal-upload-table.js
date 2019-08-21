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

import React, { useState } from 'react'
import styled from 'styled-components'
import { ProgressBarWithText } from './progress-car'

const InformalProductsContainer = styled.div`
  display: flex;
  align-items: center;
  height: 80%;
  width: 80%;
  margin-left: 10%;
  margin-top: 5%;
`

//TODO center items vertically
const InformalProductsTableStyleComp = styled.table`
  display: flex;
  flex-flow: column;
  width: 100%;
  height: 100%;
  & > thead {
    flex: 0 0 auto;
    width: calc(100% - 0.9em);
  }
  & > tbody {
    flex: 1 1 auto;
    display: block;
    overflow-y: auto;
  }
  & > tbody > tr {
    width: 100%;
  }
  & > thead,
  & > tbody > tr {
    display:table;
    table-layout: fixed;
  }

  & > tbody > tr:nth-child(odd) {
    background-color: ${props => props.theme.backgroundAccentContent};
  }
  & > tbody > tr > td {
    height: 40px;
  }
  & tr, 
  & td {
    font-size: ${props => props.theme.minimumFontSize};
    padding: ${props => props.theme.minimumFontSize};
    overflow: hidden;
    text-overflow: ellipsis;
  }
  & td,
  & th {
    padding-left: ${props => props.theme.minimumSpacing};
  }

  & th{
    font-size: ${props => props.theme.mediumFontSize};
    padding-bottom: ${props => props.theme.minimumSpacing};
  }

`

const InformalProductsTableRowStyleComp = styled.tr`
  
`

const InformalProductsTable = props => {
  const { uploads } = props

  function handleMessageClick() {
    console.log('Message was clicked')
  }

  return (
    <InformalProductsContainer>
      <InformalProductsTableStyleComp>
        <thead>
          <tr>
            <th style={{width:'50%'}}> 
              Title
            </th>
            <th style={{width:'15%'}}>
              Type
            </th>
            <th style={{width:'25%'}}>
              Status
            </th>
          </tr>
        </thead>
        <tbody>
          {props.files.map(file => {
            return (
              <InformalProductsTableRowStyleComp key={file.name}>
                <td style={{width:'50%'}}>
                  {file.name}
                </td>
                <td style={{width:'15%'}}>
                  {file.type}
                </td>
                <td style={{width:'25%'}}>
                  <ProgressBarWithText
                    progress={file.upload.progress}
                    messageOnClick={() => file.onClick(file)}
                    status={file.status}
                    message={file.message}
                  />
                </td>
              </InformalProductsTableRowStyleComp>
            )
          })}
        </tbody>
      </InformalProductsTableStyleComp>
    </InformalProductsContainer>
  )
}

export { InformalProductsTable }
