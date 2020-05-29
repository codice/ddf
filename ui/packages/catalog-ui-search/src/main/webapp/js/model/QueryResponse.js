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
const Backbone = require('backbone')
const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
const properties = require('../properties.js')
require('backbone-associations')

let rpc = null

if (properties.webSocketsEnabled && window.WebSocket) {
  const Client = require('rpc-websockets').Client
  const protocol = { 'http:': 'ws:', 'https:': 'wss:' }
  const url = `${protocol[location.protocol]}//${location.hostname}:${
    location.port
  }${location.pathname}ws`
  rpc = new Client(url)
}

module.exports = Backbone.AssociatedModel.extend({
  defaults() {
    return {
      lazyResults: undefined,
    }
  },
  url: './internal/cql',
  useAjaxSync: true,
  initialize() {
    this.listenTo(this, 'error', this.handleError)
  },
  sync(method, model, options) {
    if (rpc !== null) {
      let handled = false
      const promise = rpc
        .call('query', [options.data], options.timeout)
        .then(res => {
          if (!handled) {
            handled = true
            options.success(res)
            return [res, 'success']
          }
        })
        .catch(res => {
          if (!handled) {
            handled = true
            res.options = options
            switch (res.code) {
              case 400:
              case 404:
              case 500:
                options.error({
                  responseJSON: res,
                })
                break
              case -32000:
                if (rpc !== null) {
                  rpc.close()
                  rpc = null
                }
                options.error({
                  responseJSON: {
                    message: 'User not logged in.',
                  },
                })
                break
              default:
                // notify user and fallback to http
                if (rpc !== null) {
                  rpc.close()
                  rpc = null
                }
                options.error({
                  responseJSON: {
                    message:
                      'Search failed due to unknown reasons, please try again.',
                  },
                })
            }
            return [res, 'error']
          }
        })
      model.trigger('request', model, null, options)
      return {
        abort() {
          if (!handled) {
            handled = true
            options.error({
              responseJSON: {
                message: 'Stopped',
              },
            })
          }
        },
        promise() {
          const d = $.Deferred()
          promise
            .then(value => {
              d.resolve(value)
            })
            .catch(err => {
              d.reject(err)
            })
          return d
        },
      }
    } else {
      return Backbone.AssociatedModel.prototype.sync.call(
        this,
        method,
        model,
        options
      )
    }
  },
  handleError(resultModel, response, sent) {
    const dataJSON = JSON.parse(sent.data)
    this.get('lazyResults').updateStatusWithError({
      sources: dataJSON.srcs,
      message: response.responseJSON
        ? response.responseJSON.message
        : response.statusText,
    })
  },
  handleSync(resultModel, response, sent) {
    if (sent) {
      const dataJSON = JSON.parse(sent.data)
    }
  },
  parse(resp, options) {
    metacardDefinitions.addMetacardDefinitions(resp.types)

    this.get('lazyResults').addTypes(resp.types)
    this.get('lazyResults').updateStatus(resp.statusBySource)
    this.get('lazyResults').updateDidYouMeanFields(resp.didYouMeanFields)
    this.get('lazyResults').updateShowingResultsForFields(
      resp.showingResultsForFields
    )
    this.get('lazyResults').add({ results: resp.results })

    return {}
  },
})
