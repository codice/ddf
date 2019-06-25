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
var httpProxy = require('http-proxy')
var path = require('path')

var proxy = httpProxy.createProxyServer({
  secure: false,
  changeOrigin: true,
  autoRewrite: true,
  protocolRewrite: 'http',
})

proxy.on('proxyRes', function(proxyRes, req, res, options) {
  var cookies = proxyRes.headers['set-cookie']
  if (cookies !== undefined) {
    cookies = cookies.map(function(cookie) {
      return cookie.replace(';Secure', '')
    })
    res.set('set-cookie', cookies)
    delete proxyRes.headers['set-cookie']
  }
})

exports.requestProxy = function(req, res) {
  proxy.web(req, res, { target: 'https://localhost:8993' })
}
