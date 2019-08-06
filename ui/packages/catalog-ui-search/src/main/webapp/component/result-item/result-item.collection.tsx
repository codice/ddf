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
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'

const SHOW_MORE_LENGTH = 2

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

const ResultGroup = styled.div`
  box-shadow: none !important;
  background: inherit !important;
  > .group-representation {
    text-align: left;
    padding: ${props => props.theme.minimumSpacing} 0px;
  }

  > .group-results {
    position: relative;
    padding-left: ${props => props.theme.largeSpacing};
  }
`

const ShowingResultsForContainer = styled.div`
  padding: 0.15rem;
  text-align: center;
  font-size: 0.75rem;
  border: none !important;
`

const ShowMore = styled.a`
  padding: 0.15rem;
  font-size: 0.75rem;
`

const DidYouMeanContainer = styled.div`
  text-align: center;
  border: none !important;
`

const ResendQuery = styled.a`
  padding: 0.15rem;
  text-align: center;
  font-size: 0.75rem;
  text-decoration: none;
  width: 100%;
`

type State = {
  expandShowingResultForText: boolean
  expandDidYouMeanFieldText: boolean
}

class ResultItems extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      expandShowingResultForText: false,
      expandDidYouMeanFieldText: false,
    }
  }

  createShowResultText(showingResultsForFields: any[]) {
    let showingResultsFor = 'Showing Results for '
    if (
      showingResultsForFields !== undefined &&
      showingResultsForFields !== null &&
      showingResultsForFields.length > 0
    ) {
      if (
        !this.state.expandShowingResultForText &&
        showingResultsForFields.length > 2
      ) {
        showingResultsFor += this.createCondensedResultsForText(
          showingResultsForFields
        )
        return showingResultsFor
      }

      showingResultsFor += this.createExpandedResultsForText(
        showingResultsForFields
      )
      return showingResultsFor
    }
    return null
  }

  createDidYouMeanText(didYouMeanFields: any[]) {
    let didYouMean = 'Did you mean '
    if (
      didYouMeanFields !== undefined &&
      didYouMeanFields !== null &&
      didYouMeanFields.length > 0
    ) {
      if (
        !this.state.expandDidYouMeanFieldText &&
        didYouMeanFields.length > 2
      ) {
        didYouMean += this.createCondensedResultsForText(didYouMeanFields)
        return didYouMean
      }
      didYouMean += this.createExpandedResultsForText(didYouMeanFields)
      return didYouMean
    }
    return null
  }

  createCondensedResultsForText(showingResultsForFields: any[]) {
    const copyQuery = [...showingResultsForFields]
    copyQuery.splice(0, copyQuery.length - SHOW_MORE_LENGTH)
    return copyQuery.join(', ')
  }

  createExpandedResultsForText(showingResultsForFields: any[]) {
    return showingResultsForFields.join(', ')
  }

  rerunQuery(model: any) {
    model.set('spellcheck', false)
    model.startSearchFromFirstPage()
    model.set('spellcheck', true)
  }

  render() {
    const {
      results,
      className,
      selectionInterface,
      showingResultsForFields,
      didYouMeanFields,
      model,
    } = this.props
    if (results.length === 0) {
      return (
        <ResultItemCollection className={className}>
          <div className="result-item-collection-empty">No Results Found</div>
        </ResultItemCollection>
      )
    } else if (model.get('spellcheck')) {
      const showingResultsFor = this.createShowResultText(
        showingResultsForFields
      )
      const didYouMean = this.createDidYouMeanText(didYouMeanFields)

      return (
        <ResultItemCollection
          className={`${className} is-list has-list-highlighting`}
        >
          {showingResultsFor !== null && (
            <ShowingResultsForContainer>
              {showingResultsFor}
              {showingResultsForFields !== null &&
                showingResultsForFields !== undefined &&
                showingResultsForFields.length > 2 && (
                  <ShowMore
                    onClick={() => {
                      this.setState({
                        expandShowingResultForText: !this.state
                          .expandShowingResultForText,
                      })
                    }}
                  >
                    {this.state.expandShowingResultForText ? 'less' : 'more'}
                  </ShowMore>
                )}
            </ShowingResultsForContainer>
          )}
          {didYouMean !== null && (
            <DidYouMeanContainer>
              <ResendQuery
                onClick={() => {
                  this.rerunQuery(model)
                }}
              >
                {didYouMean}
              </ResendQuery>
              {didYouMeanFields !== null &&
                didYouMeanFields !== undefined &&
                didYouMeanFields.length > 2 && (
                  <ShowMore
                    onClick={() => {
                      this.setState({
                        expandDidYouMeanFieldText: !this.state
                          .expandDidYouMeanFieldText,
                      })
                    }}
                  >
                    {this.state.expandDidYouMeanFieldText ? 'less' : 'more'}
                  </ShowMore>
                )}
            </DidYouMeanContainer>
          )}

          {results.map(result => {
            if (result.duplicates) {
              const amount = result.duplicates.length + 1
              return (
                <ResultGroup key={result.id}>
                  <div className="group-representation">
                    {amount} duplicates
                  </div>
                  <div className="group-results global-bracket is-left">
                    <ResultItemCollection className="is-list has-list-highlighting">
                      <MarionetteRegionContainer
                        view={childView}
                        viewOptions={{
                          selectionInterface,
                          model: result,
                        }}
                        replaceElement
                      />
                      {result.duplicates.map((duplicate: any) => {
                        return (
                          <MarionetteRegionContainer
                            key={duplicate.id}
                            view={childView}
                            viewOptions={{
                              selectionInterface,
                              model: duplicate,
                            }}
                            replaceElement
                          />
                        )
                      })}
                    </ResultItemCollection>
                  </div>
                </ResultGroup>
              )
            } else {
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
            }
          })}
        </ResultItemCollection>
      )
    } else {
      return this.createResultItemCollectionView()
    }
  }

  createResultItemCollectionView() {
    const { results, selectionInterface, className } = this.props

    return (
      <ResultItemCollection
        className={`${className} is-list has-list-highlighting`}
      >
        {results.map(result => {
          if (result.duplicates) {
            const amount = result.duplicates.length + 1
            return (
              <ResultGroup key={result.id}>
                <div className="group-representation">{amount} duplicates</div>
                <div className="group-results global-bracket is-left">
                  <ResultItemCollection className="is-list has-list-highlighting">
                    <MarionetteRegionContainer
                      view={childView}
                      viewOptions={{
                        selectionInterface,
                        model: result,
                      }}
                      replaceElement
                    />
                    {result.duplicates.map((duplicate: any) => {
                      return (
                        <MarionetteRegionContainer
                          key={duplicate.id}
                          view={childView}
                          viewOptions={{
                            selectionInterface,
                            model: duplicate,
                          }}
                          replaceElement
                        />
                      )
                    })}
                  </ResultItemCollection>
                </div>
              </ResultGroup>
            )
          } else {
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
          }
        })}
      </ResultItemCollection>
    )
  }
}

export default hot(module)(ResultItems)
