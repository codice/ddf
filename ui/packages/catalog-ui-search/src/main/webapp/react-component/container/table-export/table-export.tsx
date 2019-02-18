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
  return exportSize === 'all'
    ? allData(selectionInterface)
    : visibleData(selectionInterface)
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

function getExportCount(exportSize: string, selectionInterface: any): number {
  const result = selectionInterface.getCurrentQuery().get('result')
  return exportSize === 'all'
    ? getHits(result.get('status').toJSON())
    : result.get('results').length
}

function getSorts(selectionInterface: any) {
  return selectionInterface.getCurrentQuery().get('sorts')
}

function getQueryCount(selectionInterface: any): number {
  return selectionInterface.getCurrentQuery().get('count')
}

function getWarning(exportSize: string, selectionInterface: any): string {
  const exportCount = getExportCount(exportSize, selectionInterface)
  if (exportCount > 100) {
    const queryCount = getQueryCount(selectionInterface)
    return `You are about to export ${exportCount} results. ${
      exportCount > queryCount ? `Only ${queryCount} will be exported.` : ''
    } This may take a long time.`
  }
  return ''
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
}

type Source = {
  id: string
  hits: number
}

type ExportResponse = {
  displayName: string
  id: string
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
        ],
        exportSize: 'all',
        exportFormat: 'csv',
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
    handleExportFormatChange(value: string) {
      this.setState({
        exportFormat: value,
      })
    }
    handleExportSizeChange(value: string) {
      this.setState({
        exportSize: value,
      })
    }
    async onDownloadClick() {
      const exportFormat = encodeURIComponent(this.state.exportFormat)
      try {
        const hiddenFields = getHiddenFields()
        const columnOrder = getColumnOrder()

        const cql = getCqlForSize(
          this.state.exportSize,
          this.props.selectionInterface
        )
        const sources = getSrcs(this.props.selectionInterface)
        const sorts = getSorts(this.props.selectionInterface)
        const count = Math.min(
          getExportCount(this.state.exportSize, this.props.selectionInterface),
          getQueryCount(this.props.selectionInterface)
        )
        const args = {
          hiddenFields: hiddenFields.length > 0 ? hiddenFields : [],
          columnOrder: columnOrder.length > 0 ? columnOrder : {},
          columnAliasMap: properties.attributeAliases,
        }

        const response = await exportResultSet(
          exportFormat,
          cql,
          sources,
          count,
          sorts,
          args
        )
        this.onDownloadSuccess(response)
      } catch (error) {
        console.error(error)
      }
    }
    async onDownloadSuccess(response: Response) {
      if (response.status === 200) {
        const data = await response.text()
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
      return (
        <LoadingCompanion loading={this.state.exportFormats.length === 0}>
          {this.state.exportFormats.length > 0 ? (
            <TableExportComponent
              exportFormatOptions={this.state.exportFormats}
              exportFormat={this.state.exportFormat}
              exportSizeOptions={this.state.exportSizes}
              exportSize={this.state.exportSize}
              handleExportFormatChange={this.handleExportFormatChange.bind(
                this
              )}
              handleExportSizeChange={this.handleExportSizeChange.bind(this)}
              onDownloadClick={this.onDownloadClick.bind(this)}
              warning={getWarning(
                this.state.exportSize,
                this.props.selectionInterface
              )}
            />
          ) : null}
        </LoadingCompanion>
      )
    }
  }
)
