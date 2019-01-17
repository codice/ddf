/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import { hot } from 'react-hot-loader'
import fetch from '../../utils/fetch'
import ResultsExportComponent from '../../presentation/results-export'
import { exportResult, exportResultSet } from '../../utils/export'
import { getResultSetCql } from '../../utils/cql'
import saveFile from '../../utils/save-file'

const contentDisposition = require('content-disposition')

type Result = {
  id: number
  source: string
}

type ExportFormat = {
  id: string
  displayName: string
}

type Props = {
  selectedResults: Result[]
}

type State = {
  downloadDisabled: boolean
  selectedFormat: string
  exportFormats: ExportFormat[]
}

class ResultsExport extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      selectedFormat: 'Select an export option',
      exportFormats: [],
      downloadDisabled: true,
    }
  }
  componentDidMount() {
    this.fetchExportOptions()
  }
  fetchExportOptions() {
    let transformerType = 'metacard'

    if (this.props.selectedResults.length > 1) {
      transformerType = 'query'
    }

    fetch(`./internal/transformers/${transformerType}`)
      .then(response => response.json())
      .then(exportFormats => {
        return exportFormats.sort(
          (format1: ExportFormat, format2: ExportFormat) => {
            if (format1.displayName > format2.displayName) {
              return 1
            }

            if (format1.displayName < format2.displayName) {
              return -1
            }

            return 0
          }
        )
      })
      .then(exportFormats =>
        this.setState({
          exportFormats: exportFormats,
        })
      )
  }
  getResultSources() {
    return new Set(this.props.selectedResults.map(result => result.source))
  }
  getSelectedExportFormatId() {
    const selectedFormat = this.state.selectedFormat
    let id = ''
    this.state.exportFormats.forEach(function(format) {
      if (format.displayName === selectedFormat) {
        id = format.id
      }
    })

    return id
  }
  async onDownloadClick() {
    const transformerId = this.getSelectedExportFormatId()
    let response = null

    if (this.props.selectedResults.length > 1) {
      const cql = getResultSetCql(
        this.props.selectedResults.map((result: Result) => result.id)
      )
      const sources = Array.from(this.getResultSources())

      response = await exportResultSet(transformerId, cql, sources)
    } else {
      const source = this.props.selectedResults[0].source
      const metacardId = this.props.selectedResults[0].id

      response = await exportResult(source, metacardId, transformerId)
    }

    if (response.status === 200) {
      const filename = contentDisposition.parse(
        response.headers.get('content-disposition')
      ).parameters.filename
      const contentType = response.headers.get('content-type')
      const data = await response.text()

      saveFile(filename, 'data:' + contentType, data)
    }
  }
  handleExportOptionChange(name: string) {
    this.setState({
      selectedFormat: name,
      downloadDisabled: false,
    })
  }
  render() {
    return (
      <ResultsExportComponent
        selectedFormat={this.state.selectedFormat}
        exportFormats={this.state.exportFormats}
        downloadDisabled={this.state.downloadDisabled}
        onDownloadClick={this.onDownloadClick.bind(this)}
        handleExportOptionChange={this.handleExportOptionChange.bind(this)}
      />
    )
  }
}

export default hot(module)(ResultsExport)
