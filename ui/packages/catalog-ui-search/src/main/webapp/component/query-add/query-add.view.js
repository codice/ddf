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
const $ = require('jquery');
const Marionette = require('marionette');
const template = require('./query-add.hbs');
const CustomElements = require('js/CustomElements');
const QueryBasic = require('component/query-basic/query-basic.view');
const QueryAdvanced = require('component/query-advanced/query-advanced.view');
const QueryCustom = require('component/query-custom/query-custom.view');
const QueryTitle = require('component/query-title/query-title.view');
const QueryAdhoc = require('component/query-adhoc/query-adhoc.view');
const Query = require('js/model/Query');
const store = require('js/store');
const QueryConfirmationView = require('component/confirmation/query/confirmation.query.view');
const LoadingView = require('component/loading/loading.view');
const wreqr = require('wreqr');
const user = require('component/singletons/user-instance');
const cql = require('js/cql');
const announcement = require('component/announcement');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-add'),
    regions: {
        queryContent: '> form > .editor-content > .content-form',
        queryTitle: '> form > .editor-content > .content-title',
        queryFooter: '> form > .editor-content > .content-footer'
    },
    events: {
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
        this.$el.toggleClass('is-form-builder', this.model.get('type') === 'new-form');
        switch (this.model.get('type')) {
            case 'new-form':
                this.showFormBuilder();
                break;
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
    showFormBuilder: function () {
        this.queryContent.show(new QueryAdvanced({
            model: this.model,
            isForm: true,
            isFormBuilder: true
        }));
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
        this.queryContent.show(new QueryAdvanced({
            model: this.model,
            isForm: true,
            isFormBuilder: false
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
        if (this.$el.hasClass('is-form-builder')) {
            this.saveTemplateToBackend();
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
    saveTemplateToBackend: function() {
        $.ajax({
            url: '/search/catalog/internal/forms/query',
            data: JSON.stringify(this.queryContent.currentView.getFilterTree()),
            method: 'POST',
            contentType: 'application/json',
            success: () => {
                //TODO Seriously get rid of this wreqr
                //wreqr.vent.trigger('search-form:create', this.queryContent.currentView.getFilterTree());
                announcement.announce({
                    title: 'Created!',
                    message: 'New search form has been created.',
                    type: 'success'
                });
            },
            error: () => {
                announcement.announce({
                    title: 'Error!',
                    message: 'New search form failed to be created.',
                    type: 'error'
                });
            }
        });
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