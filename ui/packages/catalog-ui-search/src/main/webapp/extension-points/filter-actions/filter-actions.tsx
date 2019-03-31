import * as React from 'react'
import { hot } from 'react-hot-loader'
import styled from '../../react-component/styles/styled-components'
import withListenTo, {
  WithBackboneProps,
} from '../../react-component/container/backbone-container'
import Enum from '../../react-component/container/input-wrappers/enum'

type Props = {
  model: Backbone.Model
  metacardDefinitions: any
} & WithBackboneProps

type State = {
  translatable: boolean
  translate: boolean
  language: string
}

const Root = styled.div`
  padding: 0px ${props => props.theme.minimumSpacing};
  display: inline-flex;
  vertical-align: middle;
  input {
    height: ${props =>
      props.theme.multiple(0.5, props.theme.minimumButtonSize, 'rem')};
    width: ${props =>
      props.theme.multiple(0.5, props.theme.minimumButtonSize, 'rem')};
  }
  label {
    display: flex;
    align-items: center;
  }
`

const LanguageSelect = styled<
  {
    translate: State['translate']
  },
  'div'
>('div')`
  display: ${props => (props.translate ? 'block' : 'none')};
`

const isTranslatable = (props: Props) => {
  console.log(props.model.toJSON())
  const type =
    props.metacardDefinitions.metacardTypes[props.model.get('type')].type
  return type === 'STRING'
}

const mapPropsToState = (props: Props): State => {
  const extensionData = props.model.get('extensionData')
  console.log(props.metacardDefinitions)
  return {
    translate: extensionData ? extensionData.translate : false,
    language: extensionData ? extensionData.language : 'English',
    translatable: isTranslatable(props),
  }
}

class FilterActions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapPropsToState(props)
  }
  componentDidMount() {
    this.props.listenTo(this.props.model, 'change', this.updateState)
  }
  updateState = () => {
    this.setState(mapPropsToState(this.props))
  }
  render() {
    const { model } = this.props
    const { translatable } = this.state
    if (translatable) {
      return (
        <Root>
          <label>
            <input
              type="checkbox"
              defaultChecked={this.state.translate}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                model.set('extensionData', {
                  ...model.get('extensionData'),
                  translate: e.currentTarget.checked,
                })
              }}
            />
            Translate
          </label>
          <LanguageSelect translate={this.state.translate}>
            <Enum
              filtering
              options={[
                {
                  label: 'English',
                  value: 'English',
                },
                {
                  label: 'Japanese',
                  value: 'Japanese',
                },
                {
                  label: 'Arabic',
                  value: 'Arabic',
                },
                {
                  label: 'German',
                  value: 'German',
                },
                {
                  label: 'Spanish',
                  value: 'Spanish',
                },
                {
                  label: 'French',
                  value: 'French',
                },
              ]}
              value={this.state.language}
              showLabel={false}
              onChange={value => {
                model.set('extensionData', {
                  ...model.get('extensionData'),
                  language: value,
                })
              }}
            />
          </LanguageSelect>
        </Root>
      )
    } else {
      return null
    }
  }
}

export default hot(module)(withListenTo(FilterActions))
