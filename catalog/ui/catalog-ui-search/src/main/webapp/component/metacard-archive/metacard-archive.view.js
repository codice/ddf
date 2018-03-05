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
define([
    'marionette',
    'underscore',
    'jquery',
    './metacard-archive.hbs',
    'js/CustomElements',
    'js/store',
    'component/loading/loading.view',
    'component/confirmation/confirmation.view',
    'js/ResultUtils',
    'js/jquery.whenAll'
], function(Marionette, _, $, template, CustomElements, store, LoadingView, ConfirmationView, ResultUtils) {

    return Marionette.ItemView.extend({
        setDefaultModel: function() {
            this.model = this.selectionInterface.getSelectedResults();
        },
        template: template,
        tagName: CustomElements.register('metacard-archive'),
        events: {
            'click button.archive': 'handleArchive',
            'click button.restore': 'handleRestore',
            'click button.archive-confirm': 'triggerConfirmedArchive'
        },
        ui: {},
        orphans: [],
        selectionInterface: store,
        initialize: function(options) {
            this.orphans = [];
            this.selectionInterface = options.selectionInterface || this.selectionInterface;
            if (!options.model) {
                this.setDefaultModel();
            }
            this.handleTypes();
        },
        handleTypes: function() {
            var types = {};
            this.model.forEach(function(result) {
                var tags = result.get('metacard').get('properties').get('metacard-tags');
                if (result.isWorkspace()) {
                    types.workspace = true;
                } else if (result.isResource()) {
                    types.resource = true;
                } else if (result.isRevision()) {
                    types.revision = true;
                } else if (result.isDeleted()) {
                    types.deleted = true;
                }
                if (result.isRemote()){
                    types.remote = true;
                }
            });
            this.$el.toggleClass('is-mixed', Object.keys(types).length > 1);
            this.$el.toggleClass('is-workspace', types.workspace !== undefined);
            this.$el.toggleClass('is-resource', types.resource !== undefined);
            this.$el.toggleClass('is-revision', types.revision !== undefined);
            this.$el.toggleClass('is-deleted', types.deleted !== undefined);
            this.$el.toggleClass('is-remote', types.remote !== undefined);
        },
        handleArchive: function() {
            var payload = JSON.stringify(this.model.map(function(result) {
                return result.get('metacard').get('id');
            }));
            this.listenTo(ConfirmationView.generateConfirmation({
                    prompt: 'Are you sure you want to archive?  Doing so will remove the item(s) future search results.',
                    no: 'Cancel',
                    yes: 'Archive'
                }),
                'change:choice',
                function(confirmation) {
                    if (confirmation.get('choice')) {
                        this.handleCheckBeforeArchive(payload);
                    }
                }.bind(this));
        },
        triggerConfirmedArchive: function() {
            var payload = JSON.stringify(this.model.map(function(result) {
                return result.get('metacard').get('id');
            }));
            this.handleConfirmedArchive(payload);
        },
        handleConfirmedArchive: function(payload) {
            var loadingView = new LoadingView();
            $.ajax({
                url: '/search/catalog/internal/metacards',
                type: 'DELETE',
                data: payload,
                contentType: 'application/json'
            }).then(function(response){
                //needed for high latency systems where refreshResults might take too long
                this.model.forEach(function(result){
                    result.get('metacard').get('properties').set('metacard-tags', ['deleted']);
                    result.trigger('refreshdata');
                });
                this.refreshResults();
            }.bind(this)).always(function(response) {
                setTimeout(function() { //let solr flush
                    loadingView.remove();
                }, 2000);
            }.bind(this));
        },
        handleOrphans: function(payload) {
            this.render();
            this.listenTo(ConfirmationView.generateConfirmation({
                prompt: 'Some results have associations not found in the list you are archiving.  Continuing will cause those associations to be orphaned.  Are you sure you want to continue?',
                no: 'Cancel',
                yes: 'Archive'
            }),
            'change:choice',
            function(confirmation) {
                if (confirmation.get('choice')) {
                    this.handleConfirmedArchive(payload);
                }
            }.bind(this));
        },
        handleCheckBeforeArchive: function(payload) {
            var loadingView = new LoadingView();
            $.ajax({
                url: '/search/catalog/internal/metacards/deletecheck',
                type: 'POST',
                data: payload,
                contentType: 'application/json'
            }).then(function(response){
                this.orphans = response['broken-links'];
                if (this.orphans.length !== 0) {
                    this.handleOrphans(payload);
                } else {
                    this.handleConfirmedArchive(payload);
                }
            }.bind(this)).always(function(response) {
                loadingView.remove();
            }.bind(this));
        },
        handleRestore: function() {
            this.listenTo(ConfirmationView.generateConfirmation({
                    prompt: 'Are you sure you want to restore?  Doing so will include the item(s) in future search results.',
                    no: 'Cancel',
                    yes: 'Restore'
                }),
                'change:choice',
                function(confirmation) {
                    if (confirmation.get('choice')) {
                        var loadingView = new LoadingView();
                        $.whenAll.apply(this, this.model.map(function(result) {
                            return $.get('/search/catalog/internal/history/' +
                                'revert/' +
                                result.get('metacard').get('properties').get('metacard.deleted.id') +
                                '/' +
                                result.get('metacard').get('properties').get('metacard.deleted.version')).then(function(response) {
                                    ResultUtils.refreshResult(result);
                            }.bind(this));
                        }.bind(this))).always(function(response) {
                            setTimeout(function() { //let solr flush
                                loadingView.remove();
                            }, 2000);
                        });
                    }
                }.bind(this));
        },
        refreshResults: function(){
            this.model.forEach(function(result){
                ResultUtils.refreshResult(result);
            });
        },
        serializeData: function() {
            return {
                orphans: this.orphans
            };
        }
    });
});