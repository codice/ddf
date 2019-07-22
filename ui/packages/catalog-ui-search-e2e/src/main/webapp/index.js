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
const { setup } = require('imperio')

const randomItem = array => {
  const n = array.length
  const i = Math.floor(Math.random() * n) + 1 - 1
  return array[i]
}

const sleep = time => new Promise(resolve => setTimeout(resolve, time))

const login = url => {
  var xhr = new XMLHttpRequest()
  xhr.open('GET', url, false, 'admin', 'admin')
  xhr.send()
}

const explore = async api => {
  for (let i = 0; i < 100; i++) {
    await sleep(1000)
    if (api.getActions().length === 0) {
      console.log('no actions')
      continue
    } else {
      break
    }
  }

  let paused = false

  window.explorer = {
    pause: () => {
      paused = true
    },
    resume: () => {
      paused = false
    },
  }

  for (let i = 0; i < 1000; i++) {
    while (paused) {
      await sleep(1000)
    }

    await sleep(1000)
    const actions = api
      .getActions()
      .filter(({ type }) => !type.match('navigation'))
      .filter(({ type }) => !type.match('workspaces/NEW-WORKSPACE'))

    if (actions.length === 0) {
      console.log('no actions')
      continue
    }

    const { type, params } = randomItem(actions)

    const param = randomItem(params || []) || {}
    console.log(type, param)
    const action = { type, ...param }
    await api.dispatch(action)
  }
}

const start = () => {
  const url = `/search/catalog/`
  login(url)
  setup(url, async api => {
    window.api = api
    await sleep(5000)
    if (window.location.search.match('explore')) {
      explore(api)
    }
  })
}

start()
