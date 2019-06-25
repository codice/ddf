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
const $ = require('jquery')

export default function saveFile(name: string, type: string, data: any) {
  if (data != null && navigator.msSaveBlob)
    return navigator.msSaveBlob(new Blob([data], { type: type }), name)
  let a = $("<a style='display: none;'/>")
  let url = window.URL.createObjectURL(new Blob([data], { type: type }))
  a.attr('href', url)
  a.attr('download', name)
  $('body').append(a)
  a[0].click()
  window.URL.revokeObjectURL(url)
  a.remove()
}

// return filename portion of content-disposition
export function getFilenameFromContentDisposition(contentDisposition: string) {
  if (contentDisposition == null) {
    return null
  }

  let parts = contentDisposition.split('=', 2)
  if (parts.length !== 2) {
    return null
  }
  return parts[1]
}
