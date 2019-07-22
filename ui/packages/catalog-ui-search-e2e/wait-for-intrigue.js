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
const https = require('https')
const { parse } = require('url')

const ping = ({ auth, url }) =>
  new Promise((resolve, reject) => {
    const { path, hostname, port } = parse(url)

    const opts = {
      auth,
      hostname,
      port,
      method: 'GET',
      rejectUnauthorized: false,
      path,
      headers: {
        'User-Agent': 'ace',
        'X-Requested-With': 'XMLHttpRequest',
        Referer: `https://${hostname}:${port}`,
      },
    }

    const req = https.request(opts, res => {
      resolve(res)
    })

    req.on('error', reject)

    req.end()
  })

const sleep = time => new Promise(resolve => setTimeout(resolve, time))

const waitFor = async url => {
  for (let i = 0; i < 60; i++) {
    await sleep(5000)

    try {
      const { statusCode, headers } = await ping({
        url,
        auth: 'admin:admin',
      })

      if (
        statusCode === 200 &&
        headers['content-type'] === 'application/json'
      ) {
        console.log(`${url} is available`)
        return
      }

      console.error(`Got status code ${statusCode} when pinging "${url}"`)
    } catch (e) {
      console.error(`Got error '${e.message}' when pinging "${url}"`)
    }
  }

  throw new Error(`Timeout waiting from ${url}`)
}

const main = async () => {
  const urls = [
    `https://localhost:${process.env.DDF_PORT ||
      8993}/search/catalog/internal/workspaces`,
    `https://localhost:${process.env.DDF_PORT ||
      8993}/search/catalog/internal/forms/query`,
    `https://localhost:${process.env.DDF_PORT ||
      8993}/search/catalog/internal/forms/result`,
  ]

  try {
    await Promise.all(urls.map(waitFor))
  } catch (e) {
    process.exit(1)
  }
}

main()
