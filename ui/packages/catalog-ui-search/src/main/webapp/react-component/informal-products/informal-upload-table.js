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

import  React, { useState } from 'react'
import styled from 'styled-components'
import {ProgressBarWithText} from './progress-car'

const InformalProductsTableStyleComp = styled.table`
    width: 100%;
    height: 100%;
    border-spacing: 0px;
    text-align: left;
    & > tbody > tr:nth-child(odd) {
        background-color: ${props => props.theme.backgroundAccentContent};
    };
    & p: {
        padding-left: ${props => props.theme.minimumSpacing};
    };
`

const InformalProductsTableRowStyleComp = styled.tr`
    padding: ${props => props.theme.minimumSpacing};
`

const InformalProductsTable = (props) => {
    const {uploads} = props

    function handleMessageClick() {
        console.log("Message was clicked");
    }

    return(
        <InformalProductsTableStyleComp>
            <col width="60%"></col>
            <col width="10%"></col>
            <col width="30%"></col>
            <thead>
                <tr>
                    <th>
                        <p>Title</p>
                    </th>
                    <th>
                        <p>Type</p>
                    </th>
                    <th> 
                        <p>Status</p>
                    </th>
                </tr>
            </thead>
            <tbody>
                {props.uploads.map((upload, index) => {
                    return(
                        <InformalProductsTableRowStyleComp>
                                <td>
                                    <p>{upload.id}</p>
                                </td>
                                <td>
                                    <p>{upload.fileType}</p>
                                </td>
                                <td>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       
                                    <ProgressBarWithText progress={upload.percentage} 
                                                         message={upload.message} 
                                                         messageOnClick={handleMessageClick}/>
                                </td>
                            </InformalProductsTableRowStyleComp>
                        )
                })}
            </tbody>
        </InformalProductsTableStyleComp>
    )
}


export {InformalProductsTable}