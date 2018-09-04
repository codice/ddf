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
/* global define */
define([
    'require',
    'marionette',
    'backbone',
    'components/iframe/iframe.view.js',
    'text!./tab-content.hbs',
    'js/wreqr.js',
    'js/CustomElements',
    'iframeresizer'
    ],function (require, Marionette, Backbone, IFrameView, template, wreqr, CustomElements) {

    return Marionette.Layout.extend({
        template: template,
        tagName: CustomElements.register('tab-content'),
        className: 'tab-pane fade',
        regions: {
            tabContentInner: '.tab-content-inner'
        },
        initialize: function(options){
            this.applicationModel = options.applicationModel;
            this.listenTo(wreqr.vent, 'application:tabShown', this.handleTabShown);
            this.listenTo(wreqr.vent, 'application:tabHidden', this.handleTabHidden);
        },
        onBeforeRender: function(){
            this.$el.attr('id', this.model.get('id'));
        },
        onRender: function(){
            var view = this;
            var iframeLocation = view.model.get('iframeLocation');
            var jsLocation = view.model.get('javascriptLocation');
            if(jsLocation){
                require([jsLocation], function(TabView){
                    var newView = new TabView({model: view.applicationModel});
                    view.tabContentInner.show(newView);
                });
            } else if(iframeLocation){
                view.tabContentInner.show(new IFrameView({
                    model: new Backbone.Model({url : iframeLocation})
                }));
            }
        },
        handleTabHidden: function(id) {
            if (id === this.model.id) {
                this.$el.removeClass('active in');
            }
        },
        handleTabShown: function(id){
            if (id === this.model.id) {
                this.$('iframe').iFrameResize();
                if (this.tabContentInner.currentView && this.tabContentInner.currentView.focus) {
                    this.tabContentInner.currentView.focus();
                }
            }
        }
    });

});