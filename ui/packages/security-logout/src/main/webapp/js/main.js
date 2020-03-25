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
/* global $ */
// eslint-disable-next-line no-extra-semi
;(function() {
  $.get('../services/platform/config/ui', function(data) {
    $('.nav img').attr('src', 'data:image/png;base64,' + data.productImage)
    $('.nav label').attr('title', data.version)
    $('.nav label:first-of-type').append(data.version)
    $('.nav label button').click(function() {
      window.location.href =
        window.location.origin +
        // eslint-disable-next-line no-undef
        (prevUrl !== undefined && prevUrl !== 'undefined'
          ? // eslint-disable-next-line no-undef
            decodeURI(prevUrl)
          : '')
    })
  })

  $.get('../services/logout/actions', function(data) {
    var action = JSON.parse(data)
    logout(action).catch(error => {
      console.log(error.message)
      $('#error-msg').show()
    })
  })
})()

function logout(action) {
  let url = action.url
  const queryParams = parseQueryParams()

  if (queryParams && queryParams['service']) {
    url = url.concat('?prevurl=').concat(queryParams['service'])
  } else if (queryParams && queryParams['prevurl']) {
    url = url
      .concat('?prevurl=')
      .concat(
        window.location.href.replace(/\/logout\/.*/, queryParams['prevurl'])
      )
  }

  window.location.href = url
}

function parseQueryParams() {
  const searchString = (window.location.search + '').split('?')

  if (searchString[1]) {
    const searchParams = searchString[1].split('&')

    return searchParams.reduce((map, param) => {
      const paramKvPair = param.split('=')

      if (paramKvPair.length === 2) {
        map[paramKvPair[0]] = decodeURIComponent(
          paramKvPair[1].split('+').join(' ')
        )
      }

      return map
    }, {})
  }
}
