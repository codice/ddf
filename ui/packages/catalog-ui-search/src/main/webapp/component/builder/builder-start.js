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
  flex-flow: column;
`

const SpacingStyle = styled.div`
  padding: ${props => props.theme.minimumSpacing};
`

const retrieveAvailableTypes = async () => {
  const response = await fetch('./internal/builder/availabletypes')
  return await response.json()
}

class BuilderStart extends React.Component {
  constructor(props) {
    super(props)
    const mds = metacardDefinitions.metacardDefinitions
    const metacardTypes = Object.keys(mds).map(card => ({
      label: card,
      value: card,
    }))
    this.state = {
      entities: metacardTypes,
      selectedType: undefined,
    }

    this.setSelectedType = this.setSelectedType.bind(this)
  }

  async componentDidMount() {
    this.setState({
      selectedType: this.state.entities[0].value,
    })
    retrievedAvailableTypes = await retrieveAvailableTypes()

    //TODO forbidden access to these right now.
    // Maybe because it is requesting localhost:8080
    // instead of 8993
    const enums = retrievedAvailableTypes.availableTypes.map(availableType => ({
      label: availableType.metacardType,
      value: availableType.metacardType,
    }))
    console.log(enums)
  }

  setSelectedType(selectedType) {
    this.setState({
      selectedType,
    })
  }

  render() {
    return (
      <div>
        <BuilderStartStyle>
          <SpacingStyle>Manually create an item.</SpacingStyle>
          <Enum
            options={this.state.entities}
            value={this.state.entities[0].value}
            filtering={true}
            label="Item Type"
            onChange={this.setSelectedType}
          />
          <SpacingStyle>
            <Button
              buttonType={buttonTypeEnum.primary}
              text="Create item"
              disabled={false}
              icon=""
              style={{ width: '100%' }}
              inText={false}
              fadeUntilHover={false}
              onClick={() => this.props.onManualSubmit(this.state.selectedType)}
            />
          </SpacingStyle>
        </BuilderStartStyle>
      </div>
    )
  }
}

export { BuilderStart }
