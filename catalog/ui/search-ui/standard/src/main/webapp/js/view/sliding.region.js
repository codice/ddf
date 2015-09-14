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
/*global define*/
/*jshint newcap:false */
define([
        'marionette',
        'underscore',
        'properties',
        'q',
        'direction'
    ],
    function (Marionette, _, Properties, Q, dir) {
    "use strict";

    var flyIn = true,
        flyOut = false;
    var Region = Marionette.Region.extend({
        currentPromise: undefined,
        initialize : function(){

        },
        //  Overridden show function that uses jquery animations to slide items in and out based on a
        // 'direction', which defaults to 'forward' or true if not present
        // views in this region should have left properties that can move them off the screen.
        show: function (view, direction) {
            var region = this;
            direction = _.isUndefined(direction, dir.forward) ? dir.forward : direction;
            this._ensureElement();

            if(!view) {
                if(this.currentView) {
                    view = this.currentView;
                } else {
                    return this.currentPromise;
                }
            }

            var isViewClosed = view.isDestroyed || _.isUndefined(view.$el);

            var isDifferentView = view !== this.currentView;
            var destroyPromise;
            // if we had a previous promise we need to chain onto it so we execute in the order we expect.
            if (!_.isUndefined(this.currentPromise) && Q.isPromise(this.currentPromise) && this.currentPromise.isPending()){
                destroyPromise = this.currentPromise;
            }
            if (isDifferentView && !destroyPromise) {
                destroyPromise = region.destroy(direction);
            } else if (isDifferentView && destroyPromise) {
                destroyPromise.then(function(){
                    return region.destroy();
                });
            } else if (!isDifferentView && !destroyPromise){
                destroyPromise = Q();
            }
            this.currentPromise = destroyPromise.then(function () {
                if (isViewClosed) {
                    view.initialize(view.options);
                    // Check if view is a Marionette view
                    if (_.isFunction(view.constructor)) {
                        view.constructor(view.options);
                    }
                    view.isDestroyed = false;
                }
                view.render();
                var openPromise;
                if (isDifferentView || isViewClosed) {
                    openPromise = region.open(view, direction, isViewClosed);
                } else {
                    openPromise = Q();
                }
                return openPromise.then(function () {
                    region.currentView = view;

                    Marionette.triggerMethod.call(region, "show", view);
                    Marionette.triggerMethod.call(view, "show");
                });


            })
                .fail(function (error) {
                    if (typeof console !== 'undefined') {
                        console.error(error.stack ? error.stack : error);
                    }
                });
            return this.currentPromise;

        },

        open: function (view, direction, isViewClosed) {
            // src  example
//           this.$el.empty().append(view.el);
            var region = this;

            this.$el.html(view.el);

            var left = 0;
            var top = 0;
            if (direction === dir.forward) {
                left = this.$el.outerWidth();
            } else if (direction === dir.backward) {
                left = -this.$el.outerWidth();
            } else if (direction === dir.upward) {
                top = this.$el.outerHeight();
            } else if (direction === dir.downward) {
                top = -this.$el.outerHeight();
            }

            if(isViewClosed) {
                view.delegateEvents(view.events);
            }

            view.$el.css({
                left : left,
                top : top,
                opacity : 0
            });

            view.isDestroyed = false;

            return this.slide(view, direction, flyIn)
                .then(function(){

                    //Child views may want to override the scroll
                    //behavior. check to see if they have specified
                    //an 'allowScroll' flag. If there is no flag present,
                    //assume they want scrolling initialized for them

                    var initScroll = true;
                    if(view.attributes){
                        initScroll = view.attributes.allowScroll;
                    }

                    if(initScroll){
                        region.$el.perfectScrollbar();
                    }else{

                        //cleanup residual scrollbar resources
                        region.$el.perfectScrollbar('destroy');
                    }
                });

        },

        // Close the current view, if there is one. If there is no
        // current view, it does nothing and returns immediately.
        destroy: function (direction) {
            var view = this.currentView;
            var region = this;
            if (!view || view.isDestroyed) {
                return Q();
            }
            this.$el.perfectScrollbar('destroy');
            return this.slide(view,direction,flyOut)
                .then(function(){
                    // call 'destroy' or 'remove', depending on which is found
                    if (view.destroy) {
                        view.destroy();
                        view.isDestroyed = true;
                    }
                    else if (view.remove) {
                        view.remove();
                        view.isDestroyed = true;
                    }

                    Marionette.triggerMethod.call(region, "destroy");

//                    delete region.currentView;
                });

        },


        slide : function(view, forwardorBackward, flyInOrOut){
            var deferred = Q.defer();
            var animationProps = {
                opacity : flyInOrOut ? 1 : 0
            };
            if (flyInOrOut === flyIn && forwardorBackward === dir.forward) {
                animationProps.left = 0;
            } else if (flyInOrOut === flyOut && forwardorBackward === dir.forward) {
                animationProps.left = parseInt(view.$el.css('left'), 10) === 0 ? -view.$el.outerWidth() : 0;
            } else if (flyInOrOut === flyIn && forwardorBackward === dir.backward) {
                animationProps.left = 0;
            } else if (flyInOrOut === flyOut && forwardorBackward === dir.backward) {
                animationProps.left = parseInt(view.$el.css('left'), 10) === 0 ? view.$el.outerWidth() : 0;
            } else if (flyInOrOut === flyIn && forwardorBackward === dir.upward) {
                animationProps.top = 0;
            } else if (flyInOrOut === flyOut && forwardorBackward === dir.upward) {
                animationProps.top = -view.$el.outerHeight();
            } else if (flyInOrOut === flyIn && forwardorBackward === dir.downward) {
                animationProps.top = 0;
            } else if (flyInOrOut === flyOut && forwardorBackward === dir.downward) {
                animationProps.top = view.$el.outerHeight();
            }

            view.$el.animate(animationProps,
                {
                    duration : Properties.slidingAnimationDuration,
                    complete: function () {
                        deferred.resolve();
                    },
                    fail: function () {
                        deferred.reject(arguments);
                    }
                });

            return deferred.promise;
        }
    });

    return Region;
});
