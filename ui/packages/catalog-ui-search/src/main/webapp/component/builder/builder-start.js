import styled from 'styled-components'
import React from 'react'
import Enum from '../../react-component/enum/'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'

// TODO make sure other metacard types aren't required
const metacardDefinitions = require('../singletons/metacard-definitions')


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

        this.setSelectedType = this.setSelectedType.bind(this);
        this.handleManualSubmit = this.handleManualSubmit.bind(this);
    }

    componentDidMount() {
        this.setState({
            selectedType: this.state.entities[0].value
        })
    }

    componentWillUnmount() {

    }

    setSelectedType(selectedType) {
        this.setState({
            selectedType
        })
    }

    handleManualSubmit () {
        this.props.onManualSubmit(this.state.selectedType)
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
                        onChange = {this.setSelectedType}
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
                        onClick={this.handleManualSubmit}/>
                    </SpacingStyle>
                </BuilderStartStyle>   
            </div>
            
        )
    }
}

export {BuilderStart}