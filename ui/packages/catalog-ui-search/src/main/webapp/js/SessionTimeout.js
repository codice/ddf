/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
const Marionette = require('marionette')
const sessionTimeoutModel = require('../component/singletons/session-timeout.js')
const BlockingLightbox = require('../component/lightbox/blocking/lightbox.blocking.view.js')
const blockingLightbox = BlockingLightbox.generateNewLightbox()
import SessionTimeout from '../react-component/presentation/session-timeout'
const SessionTimeoutView = Marionette.LayoutView.extend({
  template: () => <SessionTimeout />,
})

function showPrompt() {
  return sessionTimeoutModel.get('showPrompt')
}

sessionTimeoutModel.on('change:showPrompt', () => {
  if (showPrompt()) {
    blockingLightbox.model.updateTitle('Session Expiring')
    blockingLightbox.model.open()
    blockingLightbox.showContent(SessionTimeoutView)
  } else {
    blockingLightbox.model.close()
  }
})
