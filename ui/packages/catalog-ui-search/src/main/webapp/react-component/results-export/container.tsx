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
import { hot } from 'react-hot-loader'
import fetch from '../utils/fetch'
import ResultsExportComponent from './presentation'
import { exportResultSet } from '../utils/export'
import { getResultSetCql } from '../utils/cql'
import saveFile from '../utils/save-file'
import withListenTo, { WithBackboneProps } from '../backbone-container'
const _ = require('lodash')
const announcement = require('../../component/announcement/index.jsx')
const properties = require('../../js/properties.js')

const contentDisposition = require('content-disposition')

type ExportFormat = {
  id: string
  displayName: string
  'required-attributes': string[]
}

type Result = {
  id: string
  source: string
  attributes: string[]
}

type Props = {
  results: Result[]
  isZipped?: boolean
} & WithBackboneProps

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

  componentDidUpdate(_prevProps: Props) {
    if (
      _prevProps.results !== this.props.results ||
      _prevProps.isZipped !== this.props.isZipped
    ) {
      this.fetchExportOptions()
      this.setState({
        selectedFormat: 'Select an export option',
        downloadDisabled: true,
      })
    }
  }

  getTransformerType = () => (this.props.isZipped ? 'metacard' : 'query')

  componentDidMount() {
    this.fetchExportOptions()
  }

  fetchExportOptions = () => {
    fetch(`./internal/transformers/${this.getTransformerType()}`)
      .then(response => response.json())
      .then((exportFormats: ExportFormat[]) => {
        const resultsAttributes = Array.from(
          new Set(
            this.props.results
              .map((result: Result) => result.attributes)
              .reduce((result, arr) => result.concat(arr))
          )
        )
        return exportFormats.filter(exportFormat => {
          if (exportFormat['required-attributes'].length == 0) {
            return true
          }
          return (
            _.intersection(
              exportFormat['required-attributes'],
              resultsAttributes
            ).length == exportFormat['required-attributes'].length
          )
        })
      })
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
    return new Set(this.props.results.map((result: Result) => result.source))
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
    const count = this.props.results.length
    const cql = getResultSetCql(
      this.props.results.map((result: Result) => result.id)
    )
    const srcs = Array.from(this.getResultSources())
    const searches = [
      {
        srcs,
        cql,
        count,
      },
    ]

    const attributes = Array.from(
      new Set(
        this.props.results
          .map(result => result.attributes)
          .reduce((result, arr) => result.concat(arr))
      )
    )
    let argBody: any
    let transformer: string
    if (this.props.isZipped) {
      transformer = 'zipCompression'
      argBody = {
        columnOrder: attributes,
        columnAliasMap: properties.attributeAliases,
        transformerId: transformerId,
      }
    } else {
      transformer = transformerId
      argBody = {
        columnOrder: attributes,
        columnAliasMap: properties.attributeAliases,
      }
    }
    response = await exportResultSet(transformer, {
      searches,
      count,
      sorts: [{ attribute: 'modified', direction: 'descending' }],
      args: argBody,
    })

    if (response.status === 200) {
      const filename = contentDisposition.parse(
        response.headers.get('content-disposition')
      ).parameters.filename
      const contentType = response.headers.get('content-type')
      const data = await response.blob()

      saveFile(filename, 'data:' + contentType, data)
      const warning = response.headers.get('warning')
      if (warning) {
        announcement.announce(
          {
            title: 'Warning',
            message: warning.split('\\n'),
            type: 'warn',
          },
          15000
        )
      }
    } else {
      const data = await response.json()
      announcement.announce({
        title: 'Error',
        message: ['Could not export result(s).', (data && data.message) || ''],
        type: 'error',
      })
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
