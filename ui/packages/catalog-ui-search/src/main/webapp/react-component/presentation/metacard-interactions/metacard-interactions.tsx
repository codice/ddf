import * as React from 'react'
import styled from '../../styles/styled-components'

export const Divider = () => <div className="is-divider composed-menu" />

const InteractionIcon = styled.div`
  text-align: center;
  width: ${props => props.theme.minimumButtonSize};
  display: inline-block;
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
`

const InteractionText = styled.div`
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  display: inline-block;
`

const Interaction = styled.div`
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  white-space: nowrap;
  padding: ${props => `0px ${props.theme.minimumSpacing}`};
  cursor: pointer;
  overflow: hidden;
`

type MetacardInteractionProps = {
  help: string
  icon: string
  text: string
  onClick: (props: any) => void
  children?: any
}

export const MetacardInteraction = (props: MetacardInteractionProps) => {
  return (
    <Interaction data-help={props.help} onClick={() => props.onClick(props)}>
      <InteractionIcon className={props.icon} />
      <InteractionText>{props.text}</InteractionText>
      {props.children}
    </Interaction>
  )
}
