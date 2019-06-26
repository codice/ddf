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
import { BaseWorkerInstance } from './worker-instance.base'
var LessWorker = require('../../js/workers/less.worker.js')
var instance = new BaseWorkerInstance(new LessWorker())

import * as Backbone from 'backbone'

class LessWorkerModel extends Backbone.Model {
  getWorker(): BaseWorkerInstance {
    return this.get('worker')
  }
  defaults() {
    return {
      isRendering: false,
      worker: this.getWorker(),
    }
  }
  postMessage(data: any) {
    this.set('isRendering', true)
    this.getWorker().postMessage(data)
  }
  subscribe(callback: Function) {
    this.getWorker().subscribe(callback)
  }
  initialize() {
    this.getWorker().subscribe(() => {
      this.set('isRendering', false)
    })
  }
}
var lessWorkerModel = new LessWorkerModel({ worker: instance })

export { lessWorkerModel }
