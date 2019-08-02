import styled from '../../react-component/styles/styled-components'
import React from 'react'
import Enum from '../../react-component/container/enum/'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'

const metacardDefinitions = require('../singletons/metacard-definitions.js')


const BuilderStartStyle = styled.div`
    display: flex;
    flex-direction: column;
`

const SpacingStyle = styled.div`
    padding: ${props => props.theme.minimumSpacing};
`

class BuilderStart extends React.Component {
    constructor(props) {
        super(props);
        const mds = metacardDefinitions.metacardDefinitions;
        const metacardTypes = Object.keys(mds).map(card => ({label: card, value: card}))
        this.state = {
            entities: metacardTypes,
            selectedType: undefined
        };
    }

    componentDidMount() {
    
    }

    componentWillUnmount() {

    }

    startItemCreation() {
        console.log(this.state.selectedType);
    }

    getSelectedItem = (metacardType) => {
        this.setState({
            selectedType: metacardType
        });
    }

    render() {
        return (
            <div>
                <BuilderStartStyle>  
                    <SpacingStyle>
                    Manually create an item.
                    </SpacingStyle>
                    <Enum
                        options = {this.state.entities}
                        value = {this.state.entities[0].value}
                        filtering = {true}
                        label = "Item Type"
                        onChange = {this.getSelectedItem.bind(this)}
                    />
                    <SpacingStyle>
                        <Button 
                        buttonType={buttonTypeEnum.primary}
                        text="Create item"
                        disabled={false}
                        icon=""
                        style={{width: '100%'}}
                        inText={false}
                        fadeUntilHover={false}
                        onClick={this.startItemCreation.bind(this)}/>
                    </SpacingStyle>
                </BuilderStartStyle>   
            </div>
            
        )
    }
}

module.exports = BuilderStart