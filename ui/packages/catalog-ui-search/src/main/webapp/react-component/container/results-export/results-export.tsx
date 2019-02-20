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
import withListenTo, { WithBackboneProps } from '../backbone-container'

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
  store: any
} & WithBackboneProps

type State = {
  downloadDisabled: boolean
  selectedFormat: string
  exportFormats: ExportFormat[]
  selectedResults: Result[]
}

class ResultsExport extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      selectedFormat: 'Select an export option',
      exportFormats: [],
      downloadDisabled: true,
      ...this.mapSelectionToState(),
    }
    this.props.listenTo(
      this.props.store.getSelectedResults(),
      'update add remove reset',
      this.handleSelectionChange
    )
  }
  handleSelectionChange = () => {
    this.setState(this.mapSelectionToState())
  }
  mapSelectionToState = () => {
    const selectedResults = this.props.store
      .getSelectedResults()
      .toJSON()
      .map((result: any) => {
        return {
          id: result['metacard']['id'],
          source: result['metacard']['properties']['source-id'],
        }
      })
    return {
      selectedResults,
    }
  }
  componentDidUpdate(_prevProps: Props, prevState: State) {
    if (prevState.selectedResults !== this.state.selectedResults) {
      this.fetchExportOptions()
      this.setState({
        selectedFormat: 'Select an export option',
        downloadDisabled: true,
      })
    }
  }
  componentDidMount() {
    this.fetchExportOptions()
  }
  fetchExportOptions = () => {
    let transformerType = 'metacard'

    if (this.state.selectedResults.length > 1) {
      transformerType = 'query'
    }

    fetch(`./internal/transformers/${transformerType}`)
      .then(response => response.json())
      .then((exportFormats: ExportFormat[]) => {
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
    return new Set(this.state.selectedResults.map(result => result.source))
  }
  getSelectedExportFormatId() {
    const selectedFormat = this.state.selectedFormat
    const format = this.state.exportFormats.find(
      format => format.displayName === selectedFormat
    )

    if (format !== undefined) {
      return format.id
    }

    return undefined
  }
  async onDownloadClick() {
    const transformerId = this.getSelectedExportFormatId()

    if (transformerId === undefined) {
      return
    }

    let response = null

    if (this.state.selectedResults.length > 1) {
      const cql = getResultSetCql(
        this.state.selectedResults.map((result: Result) => result.id)
      )
      const srcs = Array.from(this.getResultSources())

      response = await exportResultSet(transformerId, {
        cql,
        srcs,
      })
    } else {
      const source = this.state.selectedResults[0].source
      const metacardId = this.state.selectedResults[0].id

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

export default hot(module)(withListenTo(ResultsExport))
