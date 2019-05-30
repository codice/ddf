import * as React from 'react'
import ResultsExport from '../results-export'
import { Props } from '.'
import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import { hot } from 'react-hot-loader'
import { getExportResults } from '../../utils/export/export'

const lightboxInstance = require('../../../component/lightbox/lightbox.view.instance.js')

const onExport = (props: Props) => {
  props.onClose()
  lightboxInstance.model.updateTitle('Export Results')
  lightboxInstance.model.open()
  lightboxInstance.showContent(
    <ResultsExport results={getExportResults(props.model)} />
  )
}

export const ExportActions = (props: Props) => {
  return (
    <MetacardInteraction
      onClick={() => onExport(props)}
      icon="fa fa-share"
      text="Export as"
      help="Starts the export process for the selected results."
    />
  )
}

export default hot(module)(ExportActions)
