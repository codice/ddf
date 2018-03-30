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
/*global require*/
var Marionette = require('marionette');
var template = require('./query-add.hbs');
var CustomElements = require('js/CustomElements');
var QueryBasic = require('component/query-basic/query-basic.view');
var QueryCustom = require('component/query-advanced/query-custom/query-custom.view');
var QueryAdvanced = require('component/query-advanced/query-advanced.view');
var QueryTitle = require('component/query-title/query-title.view');
var QueryAdhoc = require('component/query-adhoc/query-adhoc.view');
var Query = require('js/model/Query');
var store = require('js/store');
var QueryConfirmationView = require('component/confirmation/query/confirmation.query.view');
var LoadingView = require('component/loading/loading.view');
var wreqr = require('wreqr');
const cql = require('js/cql');
const user = require('component/singletons/user-instance');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-add'),
    regions: {
        queryContent: '> form > .editor-content > .content-form',
        queryTitle: '> form > .editor-content > .content-title'
    },
    events: {
        'click > form > .editor-content > .content-mode > .is-text': 'toText',
        'click > form > .editor-content > .content-mode > .is-basic': 'toBasic',
        'click > form > .editor-content > .content-mode > .is-advanced': 'toAdvanced',
        'click .editor-edit': 'edit',
        'click .editor-cancel': 'cancel',
        'click .editor-save': 'save',
        'click .editor-saveRun': 'saveRun'
    },
    initialize: function () {
        this.model = new Query.Model();
        this.listenTo(this.model, 'resetToDefaults change:type', this.reshow);
        this.listenForSave();
    },
    reshow: function() {
        switch (this.model.get('type')) {
            case 'text':
                this.showText();
                break;
            case 'basic':
                this.showBasic();
                break;
            case 'advanced':
                this.showAdvanced();
                break;
            case 'custom':
                this.showCustom();
                break;
        }
    },
    onBeforeShow: function () {
        this.reshow();
        this.showTitle();
    },
    showText: function () {
        this.queryContent.show(new QueryAdhoc({
            model: this.model
        }));
    },
    showTitle: function () {
        this.queryTitle.show(new QueryTitle({
            model: this.model
        }));
    },
    showBasic: function () {
        this.queryContent.show(new QueryBasic({
            model: this.model
        }));
    },
    handleEditOnShow: function () {
        if (this.$el.hasClass('is-editing')) {
            this.edit();
        }
    },
    showAdvanced: function () {
        this.queryContent.show(new QueryAdvanced({
            model: this.model
        }));
    },
    showCustom: function () {
        this.model.set({
            title: user.getQuerySettings().get('template').name
        });
        this.queryContent.show(new QueryCustom({
            model: this.model,
            filterTemplate: user.getQuerySettings().get('template').filterTemplate
        }));
    },
    focus: function () {
        this.queryContent.currentView.focus();
    },
    edit: function () {
        this.$el.addClass('is-editing');
        this.queryContent.currentView.edit();
    },
    cancel: function () {
        this.$el.removeClass('is-editing');
        this.onBeforeShow();
    },
    save: function () {
        this.queryContent.currentView.save();
        this.queryTitle.currentView.save();
        if (store.getCurrentQueries().get(this.model) === undefined) {
            store.getCurrentQueries().add(this.model);
        }
        this.cancel();
        this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
    },
    setDefaultTitle: function() {
        this.queryContent.currentView.setDefaultTitle();
    },
    saveRun: function () {
        this.queryContent.currentView.save();
        this.queryTitle.currentView.save();
        if (this.model.get('title') === 'Search Name') {
            this.setDefaultTitle();
        }
        if (store.getCurrentQueries().canAddQuery()) {
            store.getCurrentQueries().add(this.model);
            this.endSave();
        } else {
            this.listenTo(QueryConfirmationView.generateConfirmation({
                }),
                'change:choice',
                function (confirmation) {
                    var choice = confirmation.get('choice');
                    if (choice === true) {
                        var loadingview = new LoadingView();
                        store.get('workspaces').once('sync', function(workspace, resp, options) {
                            loadingview.remove();
                            wreqr.vent.trigger('router:navigate', {
                                fragment: 'workspaces/' + workspace.id,
                                options: {
                                    trigger: true
                                }
                            });
                        });
                        store.get('workspaces').createWorkspaceWithQuery(this.model);
                    } else if (choice !== false){
                        store.getCurrentQueries().remove(choice);
                        store.getCurrentQueries().add(this.model);
                        this.endSave();
                    }
                }.bind(this));
        }
    },
    endSave: function () {
        this.model.startSearch();
        store.setCurrentQuery(this.model);
        this.initialize();
        this.cancel();
        this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
    },
    listenForSave: function () {
        this.$el.off('saveQuery.' + CustomElements.getNamespace())
            .on('saveQuery.' + CustomElements.getNamespace(), function (e) {
                this.saveRun();
            }.bind(this));
    }
});