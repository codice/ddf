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
  transformer?: string
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
    if (
      prevState.selectedResults !== this.state.selectedResults ||
      _prevProps.transformer !== this.props.transformer
    ) {
      this.fetchExportOptions()
      this.setState({
        selectedFormat: 'Select an export option',
        downloadDisabled: true,
      })
    }
  }
  getTransformerType = () => {
    return !this.props.transformer && this.state.selectedResults.length > 1
      ? 'query'
      : 'metacard'
  }
  componentDidMount() {
    this.fetchExportOptions()
  }
  fetchExportOptions = () => {
    fetch(`./internal/transformers/${this.getTransformerType()}`)
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

    const uriEncodedTransformerId = encodeURIComponent(transformerId)
    let response = null
    const count = this.state.selectedResults.length

    if (this.props.transformer) {
      const cql = getResultSetCql(
        this.state.selectedResults.map((result: Result) => result.id)
      )
      const srcs = Array.from(this.getResultSources())
      const uriEncodedTransformerIdProp = encodeURIComponent(
        this.props.transformer
      )

      response = await exportResultSet(uriEncodedTransformerIdProp, {
        cql,
        srcs,
        count,
        args: {
          transformerId,
        },
      })
    } else if (this.state.selectedResults.length > 1) {
      const cql = getResultSetCql(
        this.state.selectedResults.map((result: Result) => result.id)
      )
      const srcs = Array.from(this.getResultSources())

      response = await exportResultSet(uriEncodedTransformerId, {
        count,
        cql,
        srcs,
      })
    } else {
      const source = this.state.selectedResults[0].source
      const uriEncodedSource = encodeURIComponent(source)
      const metacardId: any = this.state.selectedResults[0].id
      const uriEncodedMetacardId: any = encodeURIComponent(metacardId)
      response = await exportResult(
        uriEncodedSource,
        uriEncodedMetacardId,
        uriEncodedTransformerId
      )
    }

    if (response.status === 200) {
      const filename = contentDisposition.parse(
        response.headers.get('content-disposition')
      ).parameters.filename
      const contentType = response.headers.get('content-type')
      const data = await response.blob()

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
