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
    />
  )
}

export default TableExports
