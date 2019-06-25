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
export class BaseWorker {
  constructor() {
    this.defaultReply = this.defaultReply.bind(this)
    this.reply = this.reply.bind(this)
    this.onmessage = this.onmessage.bind(this)
  }
  reply(data: Object) {
    if (data instanceof Object) {
      postMessage(data)
    } else {
      throw new TypeError('reply - not enough arguments')
    }
  }
  defaultReply(message: String) {
    console.log(message)
  }
  onmessage(oEvent: any) {
    if (oEvent.data instanceof Object && oEvent.data.hasOwnProperty('method')) {
      //@ts-ignore
      this[oEvent.data.method](oEvent.data)
    } else {
      this.defaultReply(oEvent.data)
    }
  }
}
