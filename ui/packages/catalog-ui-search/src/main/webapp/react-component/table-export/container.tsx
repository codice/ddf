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
import TableExportComponent from './presentation'
import LoadingCompanion from '../loading-companion'
import { hot } from 'react-hot-loader'
const properties = require('../../js/properties.js')
import {
  ExportCountInfo,
  DownloadInfo,
} from '../../react-component/utils/export'
type Props = {
  selectionInterface: () => void
  exportFormats: Option[]
  getWarning: (exportCountInfo: ExportCountInfo) => string
  onDownloadClick: (downloadInfo: DownloadInfo) => void
  filteredAttributes: string[]
}
type Option = {
  label: string
  value: string
}
type State = {
  exportSizes: Option[]
  exportFormat: string
  exportSize: string
  customExportCount: number
}
export default hot(module)(
  class TableExport extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props)
      this.state = {
        exportSizes: [
          {
            label: 'Visible Rows',
            value: 'visible',
          },
          {
            label: 'All Rows',
            value: 'all',
          },
          {
            label: 'Exact Number of Rows',
            value: 'custom',
          },
        ],
        exportSize: 'all',
        exportFormat: 'csv',
        customExportCount: properties.exportResultLimit,
      }
    }
    transformUrl = './internal/cql/transform/'
    handleExportFormatChange = (value: string) => {
      this.setState({
        exportFormat: value,
      })
    }
    handleExportSizeChange = (value: string) => {
      this.setState({
        exportSize: value,
      })
    }
    handleCustomExportCountChange = (value: number) => {
      this.setState({ customExportCount: value })
    }
    render() {
      const {
        exportFormat,
        exportSizes,
        exportSize,
        customExportCount,
      } = this.state
      const {
        exportFormats,
        selectionInterface,
        onDownloadClick,
        getWarning,
        filteredAttributes,
      } = this.props
      return (
        <LoadingCompanion loading={exportFormats.length === 0}>
          {exportFormats.length > 0 ? (
            <TableExportComponent
              exportFormatOptions={exportFormats}
              exportFormat={exportFormat}
              exportSizeOptions={exportSizes}
              exportSize={exportSize}
              handleExportFormatChange={this.handleExportFormatChange}
              handleExportSizeChange={this.handleExportSizeChange}
              handleCustomExportCountChange={this.handleCustomExportCountChange}
              onDownloadClick={() =>
                onDownloadClick({
                  exportFormat,
                  exportSize,
                  selectionInterface,
                  customExportCount,
                  filteredAttributes,
                })
              }
              warning={getWarning({
                exportSize,
                selectionInterface,
                customExportCount,
              })}
              customExportCount={customExportCount}
            />
          ) : null}
        </LoadingCompanion>
      )
    }
  }
)
