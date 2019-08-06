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
import styled from '../../../react-component/styles/styled-components'
import { LiveProvider, LiveEditor, LiveError, LivePreview } from 'react-live'
import { hot } from 'react-hot-loader'
import alert from '../utils/alert'

import Autocomplete from '../../../react-component/container/input-wrappers/autocomplete'

const Root = styled.div`
  height: 100%;
`

class ButtonGuide extends React.Component<{}, {}> {
  constructor(props: any) {
    super(props)
  }
  getCode() {
    return `
            <div>
              <Autocomplete 
                  label="Autocomplete 1"
                  placeholder="Enter something"
                  minimumInputLength={3}
                  url="./notvalid"
              />

              <Autocomplete 
                  label="Autocomplete 2"
                  onChange={(checked) => alert(checked)}
              />
            </div>
        `
  }
  render() {
    return (
      <Root {...this.props}>
        <div className="section">
          <div className="is-header">Examples</div>
          <div className="examples is-list has-list-highlighting">
            <div className="example">
              <div className="title">Example</div>
              <LiveProvider
                code={this.getCode()}
                scope={{ Autocomplete, alert }}
              >
                <LiveEditor contentEditable={true} />
                <LiveError />
                <LivePreview style={{ padding: '20px' }} />
              </LiveProvider>
            </div>
          </div>
        </div>
      </Root>
    )
  }
}

export default hot(module)(ButtonGuide)
