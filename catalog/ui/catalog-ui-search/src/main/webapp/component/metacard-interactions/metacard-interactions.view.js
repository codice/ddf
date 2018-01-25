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
/*global define, window*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    './metacard-interactions.hbs',
    'js/CustomElements',
    'js/store',
    'component/router/router',
    'component/singletons/user-instance',
    'component/singletons/sources-instance',
    'decorator/menu-navigation.decorator',
    'decorator/Decorators',
    'js/model/Query',
    'wkx',
    'js/CQLUtils',
    'component/confirmation/query/confirmation.query.view',
    'component/loading/loading.view',
    'component/dropdown/popout/dropdown.popout.view',
    'component/result-add/result-add.view'
], function (wreqr, Marionette, _, $, template, 
    CustomElements, store, router, user, sources, 
    MenuNavigationDecorator, Decorators, Query, wkx, 
    CQLUtils, QueryConfirmationView, LoadingView, PopoutView, ResultAddView) {

    return Marionette.LayoutView.extend(Decorators.decorate({
        template: template,
        tagName: CustomElements.register('metacard-interactions'),
        className: 'is-action-list',
        modelEvents: {
            'change': 'render'
        },
        regions: {
            resultAdd: '.interaction-add'
        },
        events: {
            'click .interaction-add': 'handleAdd',
            'click .interaction-hide': 'handleHide',
            'click .interaction-show': 'handleShow',
            'click .interaction-expand': 'handleExpand',
            'click .interaction-share': 'handleShare',
            'click .interaction-download': 'handleDownload',
            'click .interaction-create-search': 'handleCreateSearch',
            'click .metacard-interaction:not(.interaction-add)': 'handleClick'
        },
        ui: {
        },
        initialize: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            this.listenTo(this.model, 'change:metacard>properties', this.onRender);
            this.listenTo(user.get('user').get('preferences').get('resultBlacklist'),
                'add remove update reset', this.checkIfBlacklisted);
        },
        onRender: function(){
            this.checkTypes();
            this.checkIsInWorkspace();
            this.checkIfDownloadable();
            this.checkIfMultiple();
            this.checkIfRouted();
            this.checkIfBlacklisted();
            this.checkHasLocation();
            this.setupResultAdd();
        },
        setupResultAdd: function() {
            this.resultAdd.show(PopoutView.createSimpleDropdown({
                componentToShow: ResultAddView,
                modelForComponent: this.model,
                leftIcon: 'fa fa-plus',
                label: 'Add / Remove from List'
            }));
        },
        handleAdd: function(e) {
            this.$el.find('.interaction-add > *').mousedown().click();
        },
        handleHide: function(){
            var preferences = user.get('user').get('preferences');
            preferences.get('resultBlacklist').add(this.model.map(function(result){
                return {
                    id: result.get('metacard').get('properties').get('id'),
                    title: result.get('metacard').get('properties').get('title')
                };
            }));
            preferences.savePreferences();
        },
        handleShow: function(){
            var preferences = user.get('user').get('preferences');
            preferences.get('resultBlacklist').remove(this.model.map(function(result){
                return result.get('metacard').get('properties').get('id');
            }));
            preferences.savePreferences();
        },
        handleExpand: function(){
            var id = this.model.first().get('metacard').get('properties').get('id');
            wreqr.vent.trigger('router:navigate', {
                fragment: 'metacards/'+id,
                options: {
                    trigger: true
                }
            });
        },
        handleShare: function(){

        },
        handleDownload: function(){
            this.model.forEach(function(result){
                var downloadUrl = result.get('metacard').get('properties').get('resource-download-url');
                if (downloadUrl !== undefined){
                    window.open(downloadUrl);
                }
            });
        },
        handleCreateSearch: function(){
            var locations = this.model.reduce((locationArray, model) => {
                let location = model.get('metacard').get('properties').get('location');
                if (location){
                    let locationGeometry = wkx.Geometry.parse(location);
                    let cqlString = "(" + CQLUtils.buildIntersectCQL(locationGeometry) + ")";
                    locationArray.push(cqlString);
                }
                return locationArray;
            }, []);
            if (locations.length === 0){
                return;  // shouldn't happen but just in case
            }
            var combinedCqlString = locations.reduce((cqlString, subCqlString, index) => {
                if (index !== 0) {
                    cqlString = cqlString + " OR ";
                }
                cqlString = cqlString + subCqlString;
                return cqlString;
            }, '');
            let cqlString = "(" + combinedCqlString + ")";
            var newQuery = new Query.Model({
                type: locations.length > 1 ? 'advanced' : 'basic'
            });
            var queryModel = store.getCurrentQueries();
            newQuery.set('cql', cqlString);
            if (queryModel.canAddQuery()){
                queryModel.add(newQuery);
                store.setCurrentQuery(newQuery);
            } else {
                this.listenTo(QueryConfirmationView.generateConfirmation({}),
                    'change:choice',
                    function (confirmation) {
                        var choice = confirmation.get('choice');
                        if (choice === true) {
                            var loadingview = new LoadingView();
                            store.get('workspaces').once('sync', function (workspace, resp, options) {
                                loadingview.remove();
                                wreqr.vent.trigger('router:navigate', {
                                    fragment: 'workspaces/' + workspace.id,
                                    options: {
                                        trigger: true
                                    }
                                });
                            });
                            store.get('workspaces').createWorkspaceWithQuery(newQuery);
                        } else if (choice !== false) {
                            store.getCurrentQueries().remove(choice);
                            store.getCurrentQueries().add(newQuery);
                            store.setCurrentQuery(newQuery);
                        }
                    }.bind(this));
            }
        },
        handleClick: function(){
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        checkIsInWorkspace: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            this.$el.toggleClass('in-workspace', Boolean(currentWorkspace));
        },
        checkIfMultiple: function(){
            this.$el.toggleClass('is-multiple', Boolean(this.model.length > 1));
        },
        checkIfRouted: function(){
            this.$el.toggleClass('is-routed', Boolean(router.toJSON().name === 'openMetacard'));
        },
        checkIfDownloadable: function() {
            var downloadable = this.model.find((result) => {
                var downloadUrl = result.get('metacard').get('properties').get('resource-download-url');
                return downloadUrl !== undefined;
            });
            this.$el.toggleClass('is-downloadable', downloadable !== undefined);
        },
        checkIfBlacklisted: function(){
            var pref = user.get('user').get('preferences');
            var blacklist = pref.get('resultBlacklist');
            var ids = this.model.map(function(result){
                return result.get('metacard').get('properties').get('id');
            });
            var isBlacklisted = false;
            ids.forEach(function(id){
                if (blacklist.get(id) !== undefined){
                    isBlacklisted = true;
                }
            });
            this.$el.toggleClass('is-blacklisted', isBlacklisted);
        },
        checkHasLocation: function(){
            var locations = this.model.reduce((locationArray, model) => {
                let location = model.get('metacard').get('properties').get('location');
                if (location){
                    let locationGeometry = wkx.Geometry.parse(location);
                    let cqlString = "(" + CQLUtils.buildIntersectCQL(locationGeometry) + ")";
                    locationArray.push(cqlString);
                }
                return locationArray;
            }, []);
            this.$el.toggleClass('has-location', locations.length > 0);
        },
        checkTypes: function(){
            var types = {};
            this.model.forEach(function(result){
                var tags = result.get('metacard').get('properties').get('metacard-tags');
                if (result.isWorkspace()){
                    types.workspace = true;
                } else if (result.isResource()){
                    types.resource = true;
                } else if (result.isRevision()){
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
        serializeData: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            var resultJSON, workspaceJSON;
            if (this.model){
                resultJSON = this.model.toJSON()
            }
            if (currentWorkspace){
                workspaceJSON = currentWorkspace.toJSON()
            }
            var result = resultJSON[0];
            return {
                remoteResourceCached: result.isResourceLocal && result.metacard.properties['source-id'] !== sources.localCatalog,
                result: resultJSON,
                workspace: workspaceJSON
            }
        }
    }, MenuNavigationDecorator));
});