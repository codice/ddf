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
import { useEffect, useState } from 'react'
import TableExport from '../../react-component/table-export'
import {
  getExportOptions,
  Transformer,
} from '../../react-component/utils/export'
const user = require('../../component/singletons/user-instance.js')
const Sources = require('../../component/singletons/sources-instance.js')
import {
  exportResultSet,
  ExportCountInfo,
  DownloadInfo,
} from '../../react-component/utils/export'
import saveFile from '../../react-component/utils/save-file'
const _ = require('underscore')
const announcement = require('../../component/announcement/index.jsx')
const properties = require('../../js/properties.js')
const contentDisposition = require('content-disposition')

type ExportResponse = {
  displayName: string
  id: string
}

export type Props = {
  selectionInterface: any
  filteredAttributes: string[]
}

type Source = {
  id: string
  hits: number
}

export function buildCqlQueryFromMetacards(metacards: any, count: any) {
  let queryParts = metacards.map((metacard: any) => {
    return `(("id" ILIKE '${metacard.metacard.id}'))`
  })
  if (count && queryParts.length > count) {
    queryParts = queryParts.slice(0, count)
  }
  return `(${queryParts.join(' OR ')})`
}
const visibleData = (size: number, selectionInterface: any) =>
  buildCqlQueryFromMetacards(
    selectionInterface.getActiveSearchResults().toJSON(),
    size
  )
const limit = 10
const allData = (selectionInterface: any) =>
  selectionInterface.getCurrentQuery().get('cql')
function getCqlForSize(
  exportType: string,
  size: number,
  selectionInterface: any
) {
  return exportType == 'custom' && size <= limit
    ? visibleData(size, selectionInterface)
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
export const getWarning = (exportCountInfo: ExportCountInfo): string => {
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

export const getDownloadBody = (downloadInfo: DownloadInfo) => {
  const {
    exportSize,
    customExportCount,
    selectionInterface,
    filteredAttributes,
  } = downloadInfo
  const hiddenFields = getHiddenFields()
  const columnOrder = getColumnOrder().filter(
    (property: string) =>
      filteredAttributes.includes(property) && !properties.isHidden(property)
  )
  const cql = getCqlForSize(exportSize, customExportCount, selectionInterface)
  const srcs = getSrcs(selectionInterface)
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
  return {
    cql,
    srcs,
    count,
    sorts,
    args,
  }
}

export const onDownloadClick = async (downloadInfo: DownloadInfo) => {
  const exportFormat = encodeURIComponent(downloadInfo.exportFormat)
  try {
    const body = getDownloadBody(downloadInfo)
    const response = await exportResultSet(exportFormat, body)
    onDownloadSuccess(response)
  } catch (error) {
    console.error(error)
  }
}
export const onDownloadSuccess = async (response: Response) => {
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

const TableExports = (props: Props) => {
  const [formats, setFormats] = useState([])

  useEffect(() => {
    const fetchFormats = async () => {
      const exportFormats = await getExportOptions(Transformer.Query)
      const sortedExportFormats = exportFormats.sort(
        (format1: ExportResponse, format2: ExportResponse) => {
          return format1.displayName.localeCompare(format2.displayName)
        }
      )
      setFormats(
        sortedExportFormats.map((exportFormat: ExportResponse) => ({
          label: exportFormat.displayName,
          value: exportFormat.id,
        }))
      )
    }
    fetchFormats()
  }, [])

  return (
    <TableExport
      exportFormats={formats}
      selectionInterface={props.selectionInterface}
      getWarning={getWarning}
      onDownloadClick={onDownloadClick}
      filteredAttributes={props.filteredAttributes}
    />
  )
}

export default TableExports
