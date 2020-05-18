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
const childView = require('./result-item.view')
import { hot } from 'react-hot-loader'
import styled from 'styled-components'
import MarionetteRegionContainer from '../../react-component/marionette-region-container'

type Props = {
  results: any[]
  selectionInterface: any
  className?: string
  showingResultsForFields: any[]
  didYouMeanFields: any[]
  userSpellcheckIsOn: boolean
  model: any
}

const ResultItemCollection = styled.div`
  padding: 0px ${props => props.theme.minimumSpacing};

  > .result-item-collection-empty {
    box-shadow: none !important;
    padding: ${props => props.theme.minimumSpacing};
    text-align: center;
  }

  > *:not(:nth-child(1)) {
    margin-top: ${props => props.theme.minimumSpacing};
  }
`

class ResultItems extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props)
  }

  render() {
    const { results, className, selectionInterface } = this.props
    if (results.length === 0) {
      return (
        <ResultItemCollection className={className}>
          <div className="result-item-collection-empty">No Results Found</div>
        </ResultItemCollection>
      )
    } else {
      return (
        <ResultItemCollection
          className={`${className} is-list has-list-highlighting`}
        >
          {results.map(result => {
            return (
              <MarionetteRegionContainer
                key={result.id}
                view={childView}
                viewOptions={{
                  selectionInterface,
                  model: result,
                }}
                replaceElement
              />
            )
          })}
        </ResultItemCollection>
      )
    }
  }
}

export default hot(module)(ResultItems)
