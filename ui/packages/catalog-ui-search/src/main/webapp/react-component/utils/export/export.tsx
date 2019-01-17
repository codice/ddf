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
import fetch from '../fetch'

export enum Transformer {
  Metacard = 'metacard',
  Query = 'query',
}

export const getExportOptions = async (type: Transformer) => {
  return await fetch(`./internal/transformers/${type}`)
}

export const exportResult = async (
  source: string,
  id: number,
  transformer: string
) => {
  return await fetch(
    `/services/catalog/sources/${source}/${id}?transform=${transformer}`
  )
}

export const exportResultSet = async (
  transformer: string,
  cql: string,
  sources: string[],
  count?: number,
  sorts?: Object[],
  args?: Object
) => {
  return await fetch(`./internal/cql/transform/${transformer}`, {
    method: 'POST',
    body: JSON.stringify({
      cql,
      srcs: sources,
      count,
      sorts,
      args,
    }),
    headers: {
      'Content-Type': 'application/json',
    },
  })
}
