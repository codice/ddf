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

import InputWithParam from '../../../react-component/container/input-wrappers/input-with-param'

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
              <InputWithParam 
                  value={{value: 'dog', distance: 5}}
                  param="without"
                  label="Input with Param 1"
              />

              <InputWithParam 
                  value={{value: 'dog', distance: 2}}
                  label="Input with Param 2"
                  param="within"
                  onChange={(value) => alert(JSON.stringify(value))}
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
              <LiveProvider code={this.getCode()} scope={{ InputWithParam }}>
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
