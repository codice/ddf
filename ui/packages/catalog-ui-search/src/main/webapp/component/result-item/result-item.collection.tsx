import * as React from 'react'
const childView = require('./result-item.view')
import { hot } from 'react-hot-loader'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'

const store = require('../../js/store.js')

const SHOW_MORE_LENGTH = 2

type Props = {
  results: any[]
  selectionInterface: any
  className?: string
  showingResultsForFields: any[]
  didYouMeanFields: any[]
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
    padding: 0.25rem;
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

const DidYouMeanContainer = styled.div`
  text-align: center;
`

const ShowMore = styled.a`
  padding: 0.25rem;
  font-size: 12px;
`

const ResendQuery = styled.a`
  padding: 0.15rem;
  box-shadow: none !important;
  text-align: center;
  font-size: 12px;
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
      showingResultsForFields !== null
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
  }

  createDidYouMeanText(didYouMeanFields: any[]) {
    let didYouMean = 'Did you mean '
    if (didYouMeanFields !== undefined && didYouMeanFields !== null) {
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
  }

  createCondensedResultsForText(showingResultsForFields: any[]) {
    const copyQuery = [...showingResultsForFields]
    copyQuery.splice(0, copyQuery.length - SHOW_MORE_LENGTH)
    return copyQuery.join(', ')
  }

  createExpandedResultsForText(showingResultsForFields: any[]) {
    return showingResultsForFields.join(', ')
  }

  rerunQuery() {
    store.getCurrentQuery().set('spellcheck', false)
    store.getCurrentQuery().startSearchFromFirstPage()
  }

  render() {
    const {
      results,
      className,
      showingResultsForFields,
      didYouMeanFields,
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
      const didYouMean = this.createDidYouMeanText(didYouMeanFields)

      return (
        <div>
          <SolrQueryDisplay className={className}>
            <div className="solr-query">
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
            </div>
          </SolrQueryDisplay>
          <DidYouMeanContainer>
            <ResendQuery
              onClick={() => {
                this.rerunQuery()
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

          {this.createResultItemCollectionView()}
        </div>
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
