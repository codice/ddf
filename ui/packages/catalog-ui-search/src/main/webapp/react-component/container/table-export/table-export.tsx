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
import { retrieveExportOptions, exportDataAs } from '../../utils/export'
import LoadingCompanion from '../loading-companion'
import saveFile from '../../utils/save-file'
import { hot } from 'react-hot-loader'
const user = require('../../../component/singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const announcement = require('../../../component/announcement/index.jsx')

function getFilenameFromContentDisposition(header: any) {
  if (header == null) {
    return null
  }

  var parts = header.split('=', 2)
  if (parts.length !== 2) {
    return null
  }
  //return filename portion
  return parts[1]
}

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
      const response = await retrieveExportOptions()
      const exportFormats = await response.json()
      this.setState({
        exportFormats: exportFormats.map((exportFormat: string) => ({
          label: exportFormat,
          value: exportFormat,
        })),
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
        const url = `${this.transformUrl}${exportFormat}`
        const hiddenFields = getHiddenFields()
        const columnOrder = getColumnOrder()
        const payload = {
          arguments: {
            hiddenFields: hiddenFields.length > 0 ? hiddenFields : {},
            columnOrder: columnOrder.length > 0 ? columnOrder : {},
            columnAliasMap: properties.attributeAliases,
          },
          cql: getCqlForSize(
            this.state.exportSize,
            this.props.selectionInterface
          ),
        }
        const response = await exportDataAs(url, payload, 'application/json')

        this.onDownloadSuccess(response)
      } catch (error) {
        console.error(error)
      }
    }
    async onDownloadSuccess(response: Response) {
      const data = await response.text()
      const status = response.status
      const contentType = response.headers.get('content-type') || undefined
      const contentDisposition =
        response.headers.get('content-disposition') || undefined
      this.saveExport(data, status, contentType, contentDisposition)
    }
    saveExport(
      data: any,
      status: number,
      contentType?: string,
      contentDisposition?: string
    ) {
      if (status === 200) {
        var filename = getFilenameFromContentDisposition(contentDisposition)
        if (filename === null) {
          filename = 'export' + Date.now()
        }
        saveFile(filename, 'data:' + contentType, data)
      } else {
        announcement.announce({
          title: 'Error!',
          message: 'Could not export results.',
          type: 'error',
        })
        console.error('Export failed with http status ' + status)
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
            />
          ) : null}
        </LoadingCompanion>
      )
    }
  }
)
