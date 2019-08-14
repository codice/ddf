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
const _ = require('underscore')
const Sources = require('../../component/singletons/sources-instance.js')

import * as React from 'react'
import { useEffect, useState } from 'react'
import TableExport from '../../react-component/table-export'
import {
  getExportOptions,
  Transformer,
} from '../../react-component/utils/export'

type Option = {
  label: string
  value: string
}

type ExportResponse = {
  displayName: string
  id: string
}

export type Props = {
  selectionInterface: any
  exportFormats: Option[]
}

function getSrcs(selectionInterface: any) {
  const srcs = selectionInterface.getCurrentQuery().get('src')
  return srcs === undefined ? _.pluck(Sources.toJSON(), 'id') : srcs
}

const TableExports = (props: Props) => {
  const [formats, setFormats] = useState([])

  useEffect(
    () => {
      async function fetchFormats() {
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
        setFormats(
          sortedExportFormats.map((exportFormat: ExportResponse) => ({
            label: exportFormat.displayName,
            value: exportFormat.id,
          }))
        )
      }
      fetchFormats()
    },
    [setFormats]
  )

  return (
    <TableExport
      getSrcs={getSrcs}
      exportFormats={formats || []}
      selectionInterface={props.selectionInterface}
    />
  )
}

export default TableExports
