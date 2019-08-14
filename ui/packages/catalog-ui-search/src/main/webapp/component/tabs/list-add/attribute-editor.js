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
import { hot } from 'react-hot-loader'
import {AttributeInput} from './attribute-input'
const metacardDefinitions = require('../../singletons/metacard-definitions.js')

const AttributeEditorContainer = styled.div`
    display: flex;
    flex-flow: column;
    height: 100%;
    width: 100%;
    overflow-y: scroll;
    background-color: ${props => props.theme.backgroundNavigation};
    border-radius: 3px;
    intrigue-property {
        padding: 0;
    }
`

const AttributeTitle = styled.div`
    padding: ${props => props.theme.minimumSpacing};
    font-size: ${props => props.theme.largeFontSize};
`

class AttributeEditor extends React.Component {
    constructor(props){
        super(props)
        this.state = {
            metacardAttributes: metacardDefinitions.metacardDefinitions
        }
    }

    render() {
        return (
            <AttributeEditorContainer>
                <AttributeTitle>Item Attributes</AttributeTitle>
                {Object.keys(this.state.metacardAttributes[this.props.metacardType]).map(key => {
                    return (<AttributeInput key={key}
                                            id={key} 
                                            hidden={this.state.metacardAttributes[this.props.metacardType][key].hidden}
                                            alias={this.state.metacardAttributes[this.props.metacardType][key].alias}
                                            multiValued={this.state.metacardAttributes[this.props.metacardType][key].multiValued}
                                            readOnly={this.state.metacardAttributes[this.props.metacardType][key].readOnly}
                                            type={this.state.metacardAttributes[this.props.metacardType][key].type}>

                            </AttributeInput>)
                })}
            </AttributeEditorContainer>
        )
    }
}

export default hot(module)(AttributeEditor)