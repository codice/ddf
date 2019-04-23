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
