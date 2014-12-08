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
/*global define, setTimeout*/
define(['jquery', 
        'underscore'
], 
function ($,_) {
    var Utils = {
        /**
         * Set up the popovers based on if the selector has a description.
         */
        setupPopOvers: function($popoverAnchor, id, title, description) {
            var selector = ".description[data-title='" + id + "']",
                options = {
                    title: title,
                    content: description,
                    trigger: 'hover'
                };
            $popoverAnchor.find(selector).popover(options);
        }, 
        /**
         * Utility that handles the starting and stopping of the refresh button spin. Callers simply provide the 
         * selector text and a function to invoke. When the function has completed, it must call done() to signal a 
         * stop spinning.
         * @param anchorSelector Selector for the refresh button.
         * @param toExec A function to invoke when the refresh button is pressed. This method should call done() when 
         *               it has finished to signal an end to the spinning.
         * @returns A utility for managing the spinning of the refresh button.
         */
        refreshButton : function(anchorSelector, toExec) {
            var self = this;
            _.extend(self, {
                expectedCalls: 2, //number of calls to done expected before stopping
                init: function() {
                    self.$button = $(anchorSelector);
                    self.counter--;
                    self.$button.on('click', self.startSpinner);
                },
                startSpinner: function() {
                    if (!self.$button.hasClass('fa-spin')) {
                        self.counter = self.expectedCalls;
                        self.$button.addClass('fa-spin');
                        setTimeout(self.done, 2000);
                        toExec();
                    }
                },
                stopSpinner: function() {
                    self.$button.removeClass('fa-spin');
                },
                done: function() {
                    self.counter--;
                    if (self.counter <= 0) {
                        self.stopSpinner();
                    }
                },
                cleanUp: function() {
                    self.$button.off('click');
                }
            });
            self.init();
            return self;
        }
    };

    return Utils;
});
