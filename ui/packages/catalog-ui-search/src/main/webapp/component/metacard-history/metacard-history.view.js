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
    './metacard-history.hbs',
    'js/CustomElements',
    'component/loading-companion/loading-companion.view',
    'js/store',
    'js/Common',
    'js/ResultUtils',
    'component/announcement',
    'moment'
], function (Marionette, _, $, template, CustomElements, LoadingCompanionView, store, Common, ResultUtils, announcement, moment) {

    var selectedVersion;

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
            this.model = this.selectionInterface.getSelectedResults().first();
        },
        template: template,
        tagName: CustomElements.register('metacard-history'),
        events: {
            'click .metacardHistory-body .metacardHistory-row': 'clickWorkspace',
            'click button': 'revertToSelectedVersion'
        },
        ui: {
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || this.selectionInterface;
            if (!options.model){
                this.setDefaultModel();
            }
            this.loadData();
        },
        loadData: function(){
            selectedVersion = undefined;
            LoadingCompanionView.beginLoading(this);
            var self = this;
            setTimeout(function(){
                $.get('./internal/history/'+self.model.get('metacard').get('id')).then(function(response){
                    self._history = response;
                }).always(function(){
                    LoadingCompanionView.endLoading(self);
                    if (!self.isDestroyed) {
                        self.render();
                    }
                });
            }, 1000);
        },
        onRender: function(){
            this.showButton();
        },
        serializeData: function(){
            var self = this;
            if (this._history){
                this._history.sort(function(historyItem1, historyItem2){
                    return (moment(historyItem2.versioned)) - (moment(historyItem1.versioned));
                });
                this._history.forEach(function(historyItem, index){
                    historyItem.niceDate = Common.getMomentDate(historyItem.versioned);
                    historyItem.versionNumber = self._history.length - index;
                });
            }
            return this._history;
        },
        highlightSelectedWorkspace: function(){
            this.$el.find('.metacardHistory-body .metacardHistory-row').removeClass('is-selected');
            this.$el.find('[data-id='+selectedVersion+']').addClass('is-selected');
        },
        clickWorkspace: function(event){
            var version = event.currentTarget;
            selectedVersion = version.getAttribute('data-id');
            this.highlightSelectedWorkspace();
            this.showButton();
        },
        showButton: function(){
            this.$el.toggleClass('has-selection',Boolean(selectedVersion));
        },
        revertToSelectedVersion: function(){
            LoadingCompanionView.beginLoading(this);
            var self = this;
            $.get('./internal/history/revert/'+this.model.get('metacard').get('id')+'/'+selectedVersion).then(function(){
                self.model.get('metacard').get('properties').set('metacard-tags', ['revision']);
                ResultUtils.refreshResult(self.model);
            }).always(function(){
                setTimeout(function(){  //let solr flush
                    LoadingCompanionView.endLoading(self);
                    self.model.trigger('refreshdata');
                    if (self.model.isRevision()){
                        announcement.announce({
                            title: 'Waiting on Reverted Data',
                            message: 'It\'s taking an unusually long time for the reverted data to come back.  The item will be put in a revisionlike state (read-only) until data returns.',
                            type: 'warn'
                        });
                    }
                    self.loadData();
                }, 2000);
            });
        }
    });
});
