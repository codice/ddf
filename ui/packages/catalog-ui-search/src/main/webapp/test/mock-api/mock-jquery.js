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
const api = require('./index')
const oldGet = $.get
const oldPost = $.post
const oldAjax = $.ajax

const mock = () => {
  const httpRequest = ({ url }) => {
    return Promise.resolve(api(url))
  }
  $.get = url => httpRequest({ url })
  $.post = httpRequest
  $.ajax = httpRequest
}

const unmock = () => {
  $.get = oldGet
  $.post = oldPost
  $.ajax = oldAjax
}

export { mock, unmock }
