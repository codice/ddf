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

type Options = {
  headers?: object
  [key: string]: unknown
}

const fetch = window.fetch
// @ts-ignore
window.__global__fetch = fetch

// patch global fetch to warn about usage during development
if (process.env.NODE_ENV !== 'production') {
  window.fetch = (...args) => {
    const error = new Error(
      [
        `Using 'window.fetch'.`,
        'Are you sure you want to do this?',
        `Most code should use 'react-component/utils/fetch' which provides defaults compatible with the backend.`,
        `To get rid of this message, use 'window.__global__fetch' instead.`,
      ].join(' ')
    )
    console.warn(error)
    return fetch(...args)
  }
}

export default function(url: string, { headers, ...opts }: Options = {}) {
  return fetch(url, {
    credentials: 'same-origin',
    cache: 'no-cache',
    ...opts,
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
      ...headers,
    },
  })
}
