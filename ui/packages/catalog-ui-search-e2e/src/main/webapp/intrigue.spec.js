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

const sleep = time => new Promise(resolve => setTimeout(resolve, time))

const login = url => {
  var xhr = new XMLHttpRequest()
  xhr.open('GET', url, false, 'admin', 'admin')
  xhr.send()
}

describe('<Intrigue />', () => {
  it('should perform a simple query', done => {
    const url = `/search/catalog/`
    login(url)
    setup(url, async api => {
      const actions = require('./intrigue-basic-query.json')

      for (let i = 0; i < actions.length; i++) {
        const action = actions[i]
        console.log(action)
        await sleep(1000)
        await api.dispatch(action)
      }

      api.cleanup()
      done()
    })
  }).timeout(60000)
})
