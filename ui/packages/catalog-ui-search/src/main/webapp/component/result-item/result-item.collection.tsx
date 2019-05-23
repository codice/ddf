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
  userSpellcheckIsOn: boolean
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
    white-space: wrap;
    word-wrap: normal;
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

type State = {
  expandSearchFieldText: boolean
}

class ResultItems extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      expandSearchFieldText: false,
    }
  }

  createShowResultText(showingResultsForFields: any[]) {
    let showingResultsFor = 'Showing Results for '
    if (
      showingResultsForFields !== undefined &&
      showingResultsForFields !== null
    ) {
      if (
        !this.state.expandSearchFieldText &&
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
  }

  createCondensedResultsForText(showingResultsForFields: any[]) {
    const copyQuery = [...showingResultsForFields]
    copyQuery.splice(0, copyQuery.length - SHOW_MORE_LENGTH)
    return copyQuery.join(', ')
  }

  createExpandedResultsForText(showingResultsForFields: any[]) {
    return showingResultsForFields.join(', ')
  }

  render() {
    const {
      results,
      className,
      showingResultsForFields,
      userSpellcheckIsOn,
    } = this.props
    if (results.length === 0) {
      return (
        <ResultItemCollection className={className}>
          <div className="result-item-collection-empty">No Results Found</div>
        </ResultItemCollection>
      )
    } else if (userSpellcheckIsOn) {
      const showingResultsFor = this.createShowResultText(
        showingResultsForFields
      )
      return (
        <SolrQueryDisplay className={className}>
          <div className="solr-query">
            {showingResultsFor}
            {showingResultsForFields !== null &&
              showingResultsForFields !== undefined &&
              showingResultsForFields.length > 2 && (
                <a
                  onClick={() => {
                    this.setState({
                      expandSearchFieldText: !this.state.expandSearchFieldText,
                    })
                  }}
                >
                  {this.state.expandSearchFieldText ? 'less' : 'more'}
                </a>
              )}
          </div>
          {this.createResultItemCollectionView()}
        </SolrQueryDisplay>
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
