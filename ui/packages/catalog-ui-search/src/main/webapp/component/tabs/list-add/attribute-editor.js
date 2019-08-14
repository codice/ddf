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
`

class AttributeEditor extends React.Component {
    constructor(props){
        super(props)
        this.state = {
            metacardType: 'common',
            metacardAttributes: metacardDefinitions.metacardDefinitions['common']
        }
        // move type to props
    }
    
    render() {
        return (
            <AttributeEditorContainer>

                {Object.keys(this.state.metacardAttributes).map(key => {
                    return (<AttributeInput key={key}
                                            id={key} 
                                            hidden={this.state.metacardAttributes[key].hidden}
                                            alias={this.state.metacardAttributes[key].alias}
                                            multiValued={this.state.metacardAttributes[key].multiValued}
                                            readOnly={this.state.metacardAttributes[key].readOnly}
                                            type={this.state.metacardAttributes[key].type}>

                            </AttributeInput>)
                })}
            </AttributeEditorContainer>
        )
    }
}

export default hot(module)(AttributeEditor)