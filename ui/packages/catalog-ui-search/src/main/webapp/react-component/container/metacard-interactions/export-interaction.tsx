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
