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
import Text from '../../../react-component/input-wrappers/text'
import Number from '../../../react-component/input-wrappers/text'

const InputContainer = styled.div`
    padding: ${props => props.theme.minimumSpacing};
`

class AttributeInput extends React.Component {
    constructor(props) {
        super(props)
    }

    getInputByType() {

        switch(this.props.type){
            case 'STRING':
                return <Text></Text>
            case 'NUMBER':
                return <Number></Number>
            default:
                return <Text></Text>
        }
    }

    render() {
        return(
            <InputContainer>
                <div>{this.props.id}</div>
                {this.getInputByType()}
            </InputContainer>
        )
    }
}

export {AttributeInput}