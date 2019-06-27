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
import { BaseWorker } from './worker.base'
;(function prepLessForWebWorker() {
  global.window = self
  global.window.less = {
    onReady: false,
  }
  global.window.document = {
    getElementsByTagName: function(tagName: any) {
      if (tagName === 'script') {
        return [
          {
            dataset: {},
          },
        ]
      } else if (tagName === 'style') {
        return []
      } else if (tagName === 'link') {
        return []
      }
    },
  }
})()

const Less = require('less')
const variableRegexPrefix = '@'
const variableRegexPostfix = '(.*:[^;]*)'

export class LessWorker extends BaseWorker {
  render(data: any) {
    let newLessStyles = data.less
    for (let key in data.theme) {
      newLessStyles = newLessStyles.replace(
        new RegExp(variableRegexPrefix + key + variableRegexPostfix),
        () => {
          return '@' + key + ': ' + data.theme[key] + ';'
        }
      )
    }
    Less.render(newLessStyles, (_unused_e: any, result: any) => {
      if (result !== undefined) {
        this.reply({
          method: 'render',
          css: result.css,
        })
      }
    })
  }
}
