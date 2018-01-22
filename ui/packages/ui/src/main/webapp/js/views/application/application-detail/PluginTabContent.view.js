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
    'icanhaz',
    'js/views/application/IFrameView.js',
    'text!pluginTabContentItemView',
    'text!pluginTabContentCollectionView',
    'js/wreqr.js',
    'iframeresizer'
    ],function (require, Marionette, Backbone, ich, IFrameView, pluginTabContentItemView, pluginTabContentCollectionView, wreqr) {

    ich.addTemplate('pluginTabContentItemView', pluginTabContentItemView);
    ich.addTemplate('pluginTabContentCollectionView', pluginTabContentCollectionView);

    var ItemView = Marionette.Layout.extend({
        template: 'pluginTabContentItemView',
        className: 'tab-pane fade',
        regions: {
            tabContentInner: '.tab-content-inner'
        },
        initialize: function(options){
            this.applicationModel = options.applicationModel;
            this.listenTo(wreqr.vent, 'application:tabShown', this.handleTabShown);
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
        handleTabShown: function(){
            this.$('iframe').iFrameResize();
        }
    });

    var PluginTabContentsView = Marionette.CollectionView.extend({
        className: 'tab-content',
        itemView: ItemView,
        itemViewOptions: function(){
            return {
                applicationModel: this.model
            };
        }
    });

    return PluginTabContentsView;
});