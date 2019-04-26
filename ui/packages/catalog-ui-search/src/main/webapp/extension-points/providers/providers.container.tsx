import * as React from 'react'
import { hot } from 'react-hot-loader'

import ThemeContainer from '../../react-component/container/theme-container'
import { IntlProvider } from 'react-intl'

const properties = require('properties')

export type Props = {
  children: React.ReactNode
}

const ProviderContainer = (props: Props) => {
  return (
    <React.Fragment>
      <ThemeContainer>
        <IntlProvider locale={navigator.language} messages={properties.i18n}>
          <>{props.children}</>
        </IntlProvider>
      </ThemeContainer>
    </React.Fragment>
  )
}

export default hot(module)(ProviderContainer)
