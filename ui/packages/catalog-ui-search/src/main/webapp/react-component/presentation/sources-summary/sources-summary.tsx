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
import * as React from "react";
import styled from "../../styles/styled-components";
import { hot } from 'react-hot-loader';

const Root = styled<{}, 'div'>('div')`
    display: block;
    width: 100%;
    height: auto;
    font-size: ${props => props.theme.largeFontSize};
    text-align: center;
    padding: ${props => props.theme.largeSpacing} 0px;
`

type Props = {
    amountDown: number;
}

function getMessage(amountDown: number) {
    switch(amountDown) {
        case 0:
        return 'All sources are currently up'
        case 1:
        return `${amountDown} source is currently down`
        default:
        return `${amountDown} sources are currently down`
    }
}

export default hot(module)(({ amountDown } : Props) => {
    return (
        <Root>
            {getMessage(amountDown)}
        </Root>
    )
})