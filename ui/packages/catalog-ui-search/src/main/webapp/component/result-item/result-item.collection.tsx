import * as React from 'react'
const childView = require('./result-item.view')
import { hot } from 'react-hot-loader'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'

const MAX_RESULT_FOR_STRING_LENGTH = 69
const SHOW_MORE_LENGTH = 11

type Props = {
  results: any[]
  selectionInterface: any
  className?: string
  solrQuery: any[]
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

const SolrQueryDisplay = styled.div`
  > .solr-query {
    padding: ${props => props.theme.minimumSpacing};
    box-shadow: none !important;
    text-align: center;
    font-size: 12px;
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

const render = (props: Props) => {
  const { results } = props
  if (results.length === 0) {
    return getNoResultsView(props)
  }
  return getResultView(props)
}

const getNoResultsView = (props: Props) => {
  const { className } = props
  return (
    <ResultItemCollection className={className}>
      <div className="result-item-collection-empty">No Results Found</div>
    </ResultItemCollection>
  )
}

const getResultView = (props: Props) => {
  const { results, selectionInterface, className, solrQuery } = props
  const showingResultsFor = createShowResultText(solrQuery)

  return (
    <SolrQueryDisplay className={className}>
      <div className="solr-query">{showingResultsFor}</div>
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
    </SolrQueryDisplay>
  )
}

function createShowResultText(solrQuery: any[]) {
  if (solrQuery !== undefined && solrQuery !== null) {
    let showingResultsFor = 'Showing Results for '
    // let showingResultsFor = solrQuery.shift() + ' '
    for (let i = 0; i < solrQuery.length - 1; i++) {
      showingResultsFor = checkMaxLength(showingResultsFor, solrQuery)
    }
    if (
      showingResultsFor.length + solrQuery[0].length <
      MAX_RESULT_FOR_STRING_LENGTH
    ) {
      return checkMaxLength(showingResultsFor, solrQuery)
    }
    return showingResultsFor
  }
}

function checkMaxLength(currentResultForText: string, solrQuery: any[]) {
  const fieldValue = solrQuery.shift()
  if (
    currentResultForText.length + fieldValue.length <
    MAX_RESULT_FOR_STRING_LENGTH - SHOW_MORE_LENGTH
  ) {
    return (currentResultForText += fieldValue)
  }
  return (currentResultForText += '... +' + (solrQuery.length + 1) + ' more')
}

export default hot(module)(render)
