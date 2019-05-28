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

const InputView = require('../input.view')
const template = require('./input-autocomplete.hbs')
const _ = require('underscore')
require('select2')

module.exports = InputView.extend({
  template,
  onRender() {
    this.initializeSelect()
    InputView.prototype.onRender.call(this)
  },
  initializeSelect() {
    const options = {
      delay: 250,
      cache: false,
      minimumInputLength: 3,
      getUrlParams(query) {
        return { q: query }
      },
      getLabel(item) {
        return item.name || item
      },
      processResults(data) {
        let items = data.items
        if (!Array.isArray(items)) {
          items = data
        }
        if (!Array.isArray(items)) {
          items = []
        }
        return items.map(item => ({
          name: item.name,
          id: item.id,
        }))
      },
    }
    _.extend(options, this.model.get('property').attributes)

    this.$el.find('select').select2({
      placeholder: options.placeholder,
      minimumInputLength: options.minimumInputLength,
      ajax: {
        url: options.url,
        dataType: 'json',
        delay: options.delay,
        data(params) {
          return options.getUrlParams(params.term)
        },
        processResults(data, params) {
          const results = options.processResults(data)
          return {
            results,
            pagination: {
              more: false,
            },
          }
        },
        cache: options.cache,
        customErrorHandling: true, // let select2 abort ajax requests silently
      },
      escapeMarkup(markup) {
        return markup
      },
      templateResult(result) {
        if (result.loading) {
          return result.text
        }
        return options.getLabel(result)
      },
      templateSelection(result) {
        if (!result.id) {
          return result.text /* nothing selected */
        }
        return options.getLabel(result)
      },
    })
  },
  getCurrentValue() {
    return this.$el.find('select').val()
  },
  onDestroy() {
    this.$el.find('select').select2('destroy')
  },
})
