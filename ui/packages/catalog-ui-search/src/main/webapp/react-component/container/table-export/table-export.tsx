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
import TableExportComponent from '../../presentation/table-export'
import {
  exportResultSet,
  getExportOptions,
  Transformer,
} from '../../utils/export'
import LoadingCompanion from '../loading-companion'
import saveFile from '../../utils/save-file'
import { hot } from 'react-hot-loader'
const _ = require('underscore')
const user = require('../../../component/singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const announcement = require('../../../component/announcement/index.jsx')
const Sources = require('../../../component/singletons/sources-instance.js')
const contentDisposition = require('content-disposition')
function buildCqlQueryFromMetacards(metacards: any) {
  const queryParts = metacards.map((metacard: any) => {
    return `(("id" ILIKE '${metacard.metacard.id}'))`
  })
  return `(${queryParts.join(' OR ')})`
}
const visibleData = (selectionInterface: any) =>
  buildCqlQueryFromMetacards(
    selectionInterface.getActiveSearchResults().toJSON()
  )
const allData = (selectionInterface: any) =>
  selectionInterface.getCurrentQuery().get('cql')
function getCqlForSize(exportSize: string, selectionInterface: any) {
  return exportSize === 'visible'
    ? visibleData(selectionInterface)
    : allData(selectionInterface)
}
function getSrcs(selectionInterface: any) {
  const srcs = selectionInterface.getCurrentQuery().get('src')
  return srcs === undefined ? _.pluck(Sources.toJSON(), 'id') : srcs
}
function getColumnOrder(): string[] {
  return user
    .get('user')
    .get('preferences')
    .get('columnOrder')
}
function getHiddenFields(): string[] {
  return user
    .get('user')
    .get('preferences')
    .get('columnHide')
}
function getHits(sources: Source[]): number {
  return sources
    .filter(source => source.id !== 'cache')
    .reduce((hits, source) => (source.hits ? hits + source.hits : hits), 0)
}
function getExportCount({
  exportSize,
  selectionInterface,
  customExportCount,
}: ExportCountInfo): number {
  if (exportSize === 'custom') {
    return customExportCount
  }
  const result = selectionInterface.getCurrentQuery().get('result')
  return exportSize === 'all'
    ? getHits(result.get('status').toJSON())
    : result.get('results').length
}
function getSorts(selectionInterface: any) {
  return selectionInterface.getCurrentQuery().get('sorts')
}
function getWarning(exportCountInfo: ExportCountInfo): string {
  const exportCount = getExportCount(exportCountInfo)
  const result = exportCountInfo.selectionInterface
    .getCurrentQuery()
    .get('result')
  const totalHits = getHits(result.get('status').toJSON())
  const limitWarning = `You cannot export more than the administrator configured limit of ${
    properties.exportResultLimit
  }.`
  let warningMessage = ''
  if (exportCount > properties.exportResultLimit) {
    if (exportCountInfo.exportSize === 'custom') {
      return limitWarning
    }
    warningMessage =
      limitWarning +
      `  Only ${properties.exportResultLimit} ${
        properties.exportResultLimit === 1 ? `result` : `results`
      } will be exported.`
  }
  if (exportCountInfo.exportSize === 'custom') {
    if (exportCount > totalHits) {
      warningMessage = `You are trying to export ${exportCount} results but there ${
        totalHits === 1 ? `is` : `are`
      } only ${totalHits}.  Only ${totalHits} ${
        totalHits === 1 ? `result` : `results`
      } will be exported.`
    }
  }
  if (
    totalHits > 100 &&
    exportCount > 100 &&
    properties.exportResultLimit > 100
  ) {
    warningMessage += `  This may take a long time.`
  }
  return warningMessage
}
type Props = {
  selectionInterface: () => void
}
type Option = {
  label: string
  value: string
}
type State = {
  exportFormats: Option[]
  exportSizes: Option[]
  exportFormat: string
  exportSize: string
  customExportCount: number
}
type Source = {
  id: string
  hits: number
}
type ExportResponse = {
  displayName: string
  id: string
}
interface ExportCountInfo {
  exportSize: string
  selectionInterface: any
  customExportCount: number
}
export default hot(module)(
  class TableExport extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props)
      this.state = {
        exportFormats: [],
        exportSizes: [
          {
            label: 'Visible',
            value: 'visible',
          },
          {
            label: 'All',
            value: 'all',
          },
          {
            label: 'Exact Number',
            value: 'custom',
          },
        ],
        exportSize: 'all',
        exportFormat: 'csv',
        customExportCount: properties.exportResultLimit,
      }
    }
    transformUrl = './internal/cql/transform/'
    async componentDidMount() {
      const response = await getExportOptions(Transformer.Query)
      const exportFormats = await response.json()
      const sortedExportFormats = exportFormats.sort(
        (format1: ExportResponse, format2: ExportResponse) => {
          if (format1.displayName > format2.displayName) {
            return 1
          }
          if (format1.displayName < format2.displayName) {
            return -1
          }
          return 0
        }
      )
      this.setState({
        exportFormats: sortedExportFormats.map(
          (exportFormat: ExportResponse) => ({
            label: exportFormat.displayName,
            value: exportFormat.id,
          })
        ),
      })
    }
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
    onDownloadClick = async () => {
      const exportFormat = encodeURIComponent(this.state.exportFormat)
      const { exportSize, customExportCount } = this.state
      const { selectionInterface } = this.props
      try {
        const hiddenFields = getHiddenFields()
        const columnOrder = getColumnOrder()
        const cql = getCqlForSize(exportSize, selectionInterface)
        const sources = getSrcs(selectionInterface)
        const sorts = getSorts(selectionInterface)
        const count = Math.min(
          getExportCount({ exportSize, selectionInterface, customExportCount }),
          properties.exportResultLimit
        )
        const args = {
          hiddenFields: hiddenFields.length > 0 ? hiddenFields : [],
          columnOrder: columnOrder.length > 0 ? columnOrder : {},
          columnAliasMap: properties.attributeAliases,
        }
        const body = {
          cql,
          srcs: sources,
          count,
          sorts,
          args,
        }
        const response = await exportResultSet(exportFormat, body)
        this.onDownloadSuccess(response)
      } catch (error) {
        console.error(error)
      }
    }
    async onDownloadSuccess(response: Response) {
      if (response.status === 200) {
        const data = await response.blob()
        const contentType = response.headers.get('content-type')
        const filename = contentDisposition.parse(
          response.headers.get('content-disposition')
        ).parameters.filename
        saveFile(filename, 'data:' + contentType, data)
      } else {
        announcement.announce({
          title: 'Error',
          message: 'Could not export results.',
          type: 'error',
        })
      }
    }
    render() {
      const { exportSize, customExportCount } = this.state
      const { selectionInterface } = this.props
      return (
        <LoadingCompanion loading={this.state.exportFormats.length === 0}>
          {this.state.exportFormats.length > 0 ? (
            <TableExportComponent
              exportFormatOptions={this.state.exportFormats}
              exportFormat={this.state.exportFormat}
              exportSizeOptions={this.state.exportSizes}
              exportSize={this.state.exportSize}
              handleExportFormatChange={this.handleExportFormatChange}
              handleExportSizeChange={this.handleExportSizeChange}
              handleCustomExportCountChange={this.handleCustomExportCountChange}
              onDownloadClick={this.onDownloadClick}
              warning={getWarning({
                exportSize,
                selectionInterface,
                customExportCount,
              })}
              customExportCount={this.state.customExportCount}
            />
          ) : null}
        </LoadingCompanion>
      )
    }
  }
)
