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
/*global define*/
define([
    'icanhaz',
    'marionette',
    'underscore',
    'jquery',
    'js/view/ModalSource.view.js',
    'js/model/Service.js',
    'wreqr',
    'text!templates/sourcePage.handlebars',
    'text!templates/sourceList.handlebars',
    'text!templates/sourceRow.handlebars'
],
function (ich,Marionette,_,$,ModalSource,Service,wreqr,sourcePage,sourceList,sourceRow) {

    var SourceView = {};

	ich.addTemplate('sourcePage', sourcePage);
	ich.addTemplate('sourceList', sourceList);
	ich.addTemplate('sourceRow', sourceRow);

	SourceView.SourceRow = Marionette.Layout.extend({
        template: "sourceRow",
        tagName: "tr",
        className: "highlight-on-hover",
        regions: {
            editModal: '.modal-container'
        },
        initialize: function(){
            _.bindAll(this);
            this.listenTo(this.model, 'change', this.render);
            this.$el.on('click', this.editSource);
        },
        serializeData: function(){
            var data = {};

            if (this.model && this.model.has('currentConfiguration')) {
              data = this.model.get('currentConfiguration').toJSON();
            }

            return data;
        },
        onBeforeClose: function() {
            this.$el.off('click');
        },
        editSource: function(evt) {
            evt.stopPropagation();
            var model = this.model;
            wreqr.vent.trigger('editSource', model);
        }
    });

    SourceView.SourceTable = Marionette.CompositeView.extend({
        template: 'sourceList',
        itemView: SourceView.SourceRow,
        itemViewContainer: 'tbody',
    });

    SourceView.SourcePage = Marionette.Layout.extend({
        template: 'sourcePage',
        events: {
            'click .refreshButton' : 'refreshSources',
            'click .addSourceLink' : 'addSource',
            'click .editLink': 'editSource'
        },
        initialize: function(){
            var view = this;
            view.listenTo(wreqr.vent, 'editSource', view.editSource);
            view.listenTo(wreqr.vent, 'refreshSources', view.refreshSources);
        },
        regions: {
            collectionRegion: '#sourcesRegion',
            sourcesModal: '#sources-modal'
        },
        onRender: function() {
            this.collectionRegion.show(new SourceView.SourceTable({ model: this.model, collection: this.model.get("collection") }));
        },
        refreshSources: function() {
            var view = this;
            view.model.get('model').fetch({
                success: function(){
                    view.model.get('collection').sort();
                    view.model.get('collection').trigger('reset');
                    view.onRender();
                }
            });
        }, 
        editSource: function(model) {
            var view = this;
            this.sourcesModal.show(new ModalSource.View(
                {
                    model: model,
                    parentModel: view.model
                })
            );
            this.sourcesModal.currentView.$el.modal();
        },
        addSource: function() {
            var view = this;
            if(view.model) {
                this.sourcesModal.show(new ModalSource.View({
                    model: view.model.getSourceModelWithServices(),
                    parentModel: view.model
                }));
                this.sourcesModal.currentView.$el.modal();
            }
        }
    });

    return SourceView;

});