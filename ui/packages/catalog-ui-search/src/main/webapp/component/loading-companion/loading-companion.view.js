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
/*global define, alert, window*/
define([
    'marionette',
    'underscore',
    'jquery',
    './loading-companion.hbs',
    'js/CustomElements',
    'js/Positioning'
], function (Marionette, _, $, template, CustomElements, Positioning) {

    var loadingCompanions = [];

    function getLoadingCompanion(linkedView) {
        return loadingCompanions.filter(function(loadingCompanion){
            return loadingCompanion.options.linkedView.cid === linkedView.cid;
        })[0];
    }

    var LoadingCompanionView = Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('loading-companion'),
        initialize: function(){
            this.render();
            if (this.options.appendTo) {
                this.options.appendTo.append(this.el);
            } else {
                $('body').append(this.el);
                this.updatePosition();
            }
            this.$el.animate({
                opacity: .6
            }, 500, function(){
                this.shown = true;
                this.$el.trigger('shown.'+this.cid);
            }.bind(this));
            this.listenTo(this.options.linkedView, 'destroy', this.destroy);
        },
        shown: false,
        stop: function(){
                this.$el.stop().animate({
                    opacity: 0
                }, 500, function(){
                    this.destroy();
                }.bind(this));
            
        },
        onDestroy: function(){
            this.$el.remove();
        },
        updatePosition: function(){
            window.requestAnimationFrame(function(){
                if (!this.isDestroyed && !this.options.linkedView.isDestroyed) {
                    var boundingBox = this.options.linkedView.el.getBoundingClientRect();
                    this.$el.css('left', boundingBox.left).css('top', boundingBox.top)
                        .css('width', boundingBox.width).css('height', boundingBox.height);
                    this.$el.toggleClass('is-hidden', Positioning.isEffectivelyHidden(this.options.linkedView.el));
                    this.updatePosition();
                }
            }.bind(this));
        }
    });

    return {
        beginLoading: function(linkedView, appendTo){
            if (!linkedView){
                throw "Must pass the view you're calling the loader from.";
            }
            // only start loader if the view hasn't already been destroyed.
            if (!linkedView.isDestroyed) {
                if (!getLoadingCompanion(linkedView)) {
                    loadingCompanions.push(new LoadingCompanionView({
                        linkedView: linkedView,
                        appendTo: appendTo
                    }));
                }
            }
        },
        endLoading: function(linkedView){
            if (!linkedView){
                throw "Must pass the view you're called the loader from.";
            }
            var loadingCompanion = getLoadingCompanion(linkedView);
            if (loadingCompanion){
                loadingCompanion.stop();
                loadingCompanions.splice(loadingCompanions.indexOf(loadingCompanion), 1);
            }
        }
    };
});