import styled from '../../react-component/styles/styled-components'
import React from 'react'
import fetch from '../../react-component/utils/fetch'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import Enum from '../../react-component/container/Enum/'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'

const metacardDefinitions = require('../singletons/metacard-definitions.js')
const DropdownView = require('../dropdown/dropdown.view')

class BuilderStart extends React.Component {
    constructor(props) {
        super(props);
        const mds = metacardDefinitions.metacardDefinitions;
        const metacardTypes = Object.keys(mds).map(card => ({label: card, value: card}))
        this.state = {
            entities: metacardTypes
        };
    }

    componentDidMount() {
    
    }

    componentWillUnmount() {

    }

    startItemCreation() {
        
    }

    render() {
        return (
            <div>   
                Manually create an item.
                <Enum
                    options = {this.state.entities}
                    value = {this.state.entities[0].value}
                    filtering = {true}
                    label = "Item Type"
                />
                <Button 
                buttonType={buttonTypeEnum.primary}
                text="create item"
                disabled={false}
                icon=""
                style={{padding: '0px 10px'}}
                inText={false}
                fadeUntilHover={false}
                onClick={this.startItemCreation}>
                />
            </div>
            
        )
    }
}

module.exports = BuilderStart