import * as React from 'react'
import ResultsExport from '../results-export'
import { Props } from '.'
import { MetacardInteraction } from '../../presentation/metacard-interactions/metacard-interactions'
import { hot } from 'react-hot-loader'

const store = require('../../../js/store.js')
const lightboxInstance = require('../../../component/lightbox/lightbox.view.instance.js')

const onExport = (props: Props) => {
  props.onClose()
  lightboxInstance.model.updateTitle('Export Results')
  lightboxInstance.model.open()
  lightboxInstance.showContent(<ResultsExport store={store} />)
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
