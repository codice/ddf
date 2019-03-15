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
import * as React from 'react'
import withListenTo, { WithBackboneProps } from '../../container/backbone-container'
const user = require('../../../component/singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const Property = require('../../../component/property/property.js')
const PropertyView = require('../../../component/property/property.view.js')
import MarionetteRegionContainer from '../../../react-component/container/marionette-region-container'
// import styled from '../../styles/styled-components'
// import { CustomElement } from '../../styles/mixins'
// import { ChangeBackground } from '../../styles/mixins'
import { hot } from 'react-hot-loader'
// import user from '../user';
// const CustomElements = require('../../../js/CustomElements.js')

// const Root = styled.div`
//    background-color: red;
// `

type Props = {
    onClose : () => void
} & WithBackboneProps

type State = {
    value: Number
}

class SearchSettings extends React.Component<Props, State>{
    propertyModel: any
    constructor (props: Props) {
        super(props)
        this.state = {
            value: this.getUserResultCount()
        }
        
    }
    render () {
        const propertyModel = new Property({
            label: 'Number of Search Results',
            value: [this.state.value],
            min: 1,
            max: properties.resultCount,
            type: 'RANGE',
            isEditing: true
        })
        console.log('state', this.state)
        return (
            <div>
                <div className="editor-properties">
                    <div className="property-result-count">
                        <MarionetteRegionContainer
                            view = {PropertyView}
                            viewOptions = {{
                                model: propertyModel,
                            }}
                            replaceElement
                        />
                    </div>
                    <div className="is-header">Defaults</div>
                    <div className="property-search-settings">
                    </div>
                </div>
                <div className="editor-footer">
                    <button className="editor-cancel is-negative" onClick={this.triggerCancel}>
                        <span className="fa fa-times"></span>
                        <span>
                            Cancel
                        </span>
                    </button>
                        <button className="editor-save is-positive" onClick={this.triggerSave}>
                        <span className="fa fa-floppy-o">
                        </span>
                        <span>
                            Save
                        </span>
                    </button>
                </div>
            </div>
        )
    }
    triggerSave = () => {
        console.log('value: ', this.propertyModel.getValue()[0])
    }
    triggerCancel = () => {
        console.log('cancelling!')
        this.setState({
            value: 100
        })
        this.props.onClose()
    }

    getUserResultCount = () => {
        return user.get('user').get('preferences').get('resultCount')
    }
}

export default hot(module)(withListenTo(SearchSettings))