import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import { buttonTypeEnum, Button } from '../button'

type Props = {
  remove: () => void
  navigate: () => void
  itemTitle: string
}

/* TODO: Add back in css transitions */

const Root = styled<Props, 'div'>('div')`
  display: block;
  height: ${props =>
    props.theme.minimumButtonSize + props.theme.minimumSpacing};
  text-align: center;
  margin-bottom: ${props => props.theme.minimumSpacing};
  cursor: pointer;

  .item-details {
    vertical-align: top;
    padding: 0px ${props => props.theme.minimumSpacing};
    text-align: center;
    width: calc(100% - 2 * ${props => props.theme.minimumButtonSize});
    height: ${props => props.theme.minimumButtonSize};
    line-height: ${props => props.theme.minimumButtonSize};
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    display: inline-block;
  }
  .button-remove {
    float: right;
  }
`

const render = (props: Props) => {
  return (
    <Root {...props}>
      <div className="item-details" onClick={props.navigate}>
        {props.itemTitle}
      </div>
      <Button
        className="button-remove"
        icon="fa fa-eye"
        buttonType={buttonTypeEnum.neutral}
        onClick={props.remove}
      />
    </Root>
  )
}
export default hot(module)(render)
