import * as React from 'react'
import { hot } from 'react-hot-loader'

import NavigatorContainer from './navigator.container'
import {
  Props as PresentationProps,
  ProductLink,
  Divider,
  UpperNavigationLinks,
  RecentLinks,
  LowerNavigationLinks,
  Root,
} from './navigator.presentation'

export type Props = {
  closeSlideout?: () => void
}

const Navigator = (props: Props) => (
  <NavigatorContainer closeSlideout={props.closeSlideout}>
    {(props: PresentationProps) => {
      return (
        <Root {...props}>
          <ProductLink {...props} />
          <Divider />
          <UpperNavigationLinks {...props} />
          {props.recentMetacard || props.recentWorkspace ? <Divider /> : ''}
          <RecentLinks {...props} />
          <Divider />
          <LowerNavigationLinks {...props} />
        </Root>
      )
    }}
  </NavigatorContainer>
)

export default hot(module)(Navigator)
