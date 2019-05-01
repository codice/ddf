import * as React from 'react'
const childView = require('./result-item.view')
import { hot } from 'react-hot-loader'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'

type Props = {
  results: any[]
  selectionInterface: any
  className?: string
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

const render = (props: Props) => {
  const { results, selectionInterface, className } = props
  if (results.length === 0) {
    return (
      <ResultItemCollection className={className}>
        <div className="result-item-collection-empty">No Results Found</div>
      </ResultItemCollection>
    )
  }
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

export default hot(module)(render)
