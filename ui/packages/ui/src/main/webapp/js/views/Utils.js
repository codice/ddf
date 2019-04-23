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

define(['jquery', 'underscore'], function($, _) {
  const Utils = {
    /**
     * Utility that handles the starting and stopping of the refresh button spin. Callers simply provide the
     * selector text and a function to invoke. When the function has completed, it must call done() to signal a
     * stop spinning. Once the parent view
     * @param anchorSelector Selector for the refresh button.
     * @param toExec A function to invoke when the refresh button is pressed. This method should call done() when
     *               it has finished to signal an end to the spinning.
     * @param view The view that is responding to the refresh click.
     * @returns A utility for managing the spinning of the refresh button.
     */
    refreshButton: function(anchorSelector, toExec, view) {
      const self = this;
      self.initialized = false
      _.extend(self, {
        view: view,
        ts: new Date().getTime(),
        expectedCalls: 2, //number of calls to done expected before stopping
        init: function() {
          if (!self.initialized) {
            self.$button = view.$(anchorSelector) //use view here to limit query
            self.counter--
            self.$button.on('click', self.startSpinner)
            self.ts = Date.now()
            self.initialized = true
          }
        },
        startSpinner: function() {
          if (!self.$button.hasClass('fa-spin')) {
            self.counter = self.expectedCalls
            self.$button.addClass('fa-spin')
            setTimeout(self.done, 2000)
            toExec()
          }
        },
        stopSpinner: function() {
          self.$button.removeClass('fa-spin')
        },
        done: function() {
          self.counter--
          if (self.counter <= 0) {
            self.stopSpinner()
          }
        },
        cleanUp: function() {
          self.$button.off('click')
        },
      })
      return self
    },
  };

  return Utils
})
