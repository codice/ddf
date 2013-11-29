/*global define*/
/*jshint newcap:false */
define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        _ = require('underscore'),
        Properties = require('properties'),
        Q = require('q'),
        dir = require('direction');

    require('jqueryui');
    var flyIn = true,
        flyOut = false;
    var Region = Marionette.Region.extend({
        initialize : function(){

        },
        //  Overridden show function that uses jquery animations to slide items in and out based on a
        // 'direction', which defaults to 'forward' or true if not present
        // views in this region should have left properties that can move them off the screen.
        show: function (view, direction) {
            var region = this;
            direction = _.isUndefined(direction, dir.forward) ? dir.forward : direction;
            this.ensureEl();

            var isViewClosed = view.isClosed || _.isUndefined(view.$el);

            var isDifferentView = view !== this.currentView;
            var closePromise;
            if (isDifferentView) {
                closePromise = region.close(direction);
            } else {
                closePromise = Q();
            }
            return closePromise.then(function () {
                view.render();
                var openPromise;
                if (isDifferentView || isViewClosed) {
                    openPromise = region.open(view, direction);
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
                    console.error(error.stack ? error.stack : error);
                });

        },

        open: function (view, direction) {
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

            view.$el.css({
                left : left,
                top : top,
                opacity : 0
            });

            return this.slide(view, direction, flyIn)
                .then(function(){
                    region.$el.perfectScrollbar();
                });

        },

        // Close the current view, if there is one. If there is no
        // current view, it does nothing and returns immediately.
        close: function (direction) {
            var view = this.currentView;
            var region = this;
            if (!view || view.isClosed) {
                return Q();
            }
            this.$el.perfectScrollbar('destroy');
            return this.slide(view,direction,flyOut)
                .then(function(){
                    // call 'close' or 'remove', depending on which is found
                    if (view.close) {
                        view.close();
                    }
                    else if (view.remove) {
                        view.remove();
                    }

                    Marionette.triggerMethod.call(region, "close");

                    delete region.currentView;
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
