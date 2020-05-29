import * as React from 'react'
import { hot } from 'react-hot-loader'
import { useSelectedResults } from 'catalog-ui-search/src/main/webapp/js/model/LazyQueryResult/hooks'
import { useLazyResultsFromSelectionInterface } from '../../selection-interface/hooks'
import MRC from '../../../react-component/marionette-region-container'
const InspectorView = require('./inspector.view')

type Props = {
  selectionInterface: any
}

const LazyInspector = ({ selectionInterface }: Props) => {
  const lazyResults = useLazyResultsFromSelectionInterface({
    selectionInterface,
  })
  const selectedResults = useSelectedResults({
    lazyResults,
  })
  const backboneModels = Object.values(selectedResults).map(result => {
    return result.getBackbone()
  })
  selectionInterface.setSelectedResults(backboneModels)
  return (
    <MRC
      key="inspector"
      view={InspectorView}
      viewOptions={{
        selectionInterface,
      }}
    />
  )
}

export default hot(module)(LazyInspector)
