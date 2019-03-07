import * as React from 'react'
import styled from '../../../react-component/styles/styled-components'

const Empty = styled.div`
  text-align: center;
  padding: ${props => props.theme.largeSpacing};
  display: none;
  span {
    color: ${props => props.theme.warningColor};
  }
`
const Attribute = styled.div`
  display: block;
  opacity: 1;
  transition: opacity ${props => props.theme.coreTransitionTime} linear;
  transform: translateX(0%);
`
const NoData = styled.div`
  text-align: center;
  padding: ${props => props.theme.largeSpacing};
  display: none;
  span {
    color: ${props => props.theme.warningColor};
  }
`
const Container = styled.div`
  display: block;
  height: ~'calc(100% - 135px)';
  opacity: 1;
  transition: opacity ${props => props.theme.coreTransitionTime} linear;
  transform: translateX(0%);
`
const Warning = styled.span`
  color: ${props => props.theme.warningColor};
`

export const HistogramContainer = () => (
  <React.Fragment>
    <Empty className="histogram-empty">
      <h3>Please select a result set to display on the histogram.</h3>
    </Empty>
    <Attribute className="histogram-attribute" />
    <NoData className="histogram-no-matching-data">
      <h3>
        <Warning className="fa fa-exclamation-triangle" />
        Nothing in the current result set contains this attribute.
      </h3>
    </NoData>
    <Container className="histogram-container" />
  </React.Fragment>
)
