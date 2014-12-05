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
    'q',
    'js/view/ModalSource.view.js',
    'js/model/Service.js',
    'js/model/Status.js',
    'wreqr',
    'text!templates/deleteModal.handlebars',
    'text!templates/deleteSource.handlebars',
    'text!templates/sourcePage.handlebars',
    'text!templates/sourceList.handlebars',
    'text!templates/sourceRow.handlebars'
],
function (ich,Marionette,_,$,Q,ModalSource,Service,Status,wreqr,deleteModal,deleteSource,sourcePage,sourceList,sourceRow) {

    var SourceView = {};

    ich.addTemplate('deleteModal', deleteModal);
    ich.addTemplate('deleteSource', deleteSource);
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
        events: {
            'change .configurationSelect' : 'changeConfiguration',
            'click .configurationSelect' : 'handleSelector',
            'click td' : 'editSource'
        },
        initialize: function(){
            _.bindAll(this);
            this.listenTo(this.model, 'change', this.render);
            this.listenTo(wreqr.vent, 'status:update', this.updateStatus);
            this.getInitialStatuses();
        },
        getInitialStatuses: function() {
            var statusModel = new Status.List();
            statusModel.fetch({
                success: function() {
                    wreqr.vent.trigger('status:update', statusModel);
                }
            });
        },
        serializeData: function(){
            var data = {};

            if(this.model && this.model.has('currentConfiguration')){
                data.currentConfiguration = this.model.get('currentConfiguration').toJSON();
            }
            if(this.model && this.model.has('disabledConfigurations')){
                data.disabledConfigurations = this.model.get('disabledConfigurations').toJSON();
            }
            data.available = this.model.get('available');
            data.name = this.model.get('name');

            return data;
        },
        handleSelector: function(evt) {
            evt.stopPropagation();
        },
        editSource: function(evt) {
            evt.stopPropagation();
            var model = this.model;
            wreqr.vent.trigger('editSource', model);
        },
        changeConfiguration: function(evt) {
            var model = this.model;
            var currentConfig = model.get('currentConfiguration');
            var disabledConfigs = model.get('disabledConfigurations');
            var $select = $(evt.currentTarget);
            var optionSelected = $select.find("option:selected");
            var valueSelected = optionSelected.val();
            var cfgToDisable;

            if (valueSelected === 'Disabled') {
                cfgToDisable = currentConfig;
                if (!_.isUndefined(cfgToDisable)) {
                    cfgToDisable.makeDisableCall();
                    model.removeConfiguration(cfgToDisable);
                }
            } else {
                var cfgToEnable = disabledConfigs.find(function(cfg) {
                    return valueSelected + "_disabled" === cfg.get('fpid');
                });

                if (cfgToEnable) {
                    cfgToDisable = currentConfig;
                    cfgToEnable.makeEnableCall();
                    model.removeConfiguration(cfgToEnable);
                    if (!_.isUndefined(cfgToDisable)) {
                        cfgToDisable.makeDisableCall();
                        model.removeConfiguration(cfgToDisable);
                    }
                }
            }
            wreqr.vent.trigger('refreshSources');
            evt.stopPropagation();
        },
        updateStatus: function(statusList) {
            var model = this.model;
            var currentStatus = statusList.find(function(status) {
                return status.id === model.id;
            });
            var available = model.get('available');
            if (currentStatus) {
                var curAvail = currentStatus.get('available');
                if (available !== curAvail) {
                    model.set('available', curAvail);
                    this.render();
                }
            }
        }
    });

    SourceView.SourceTable = Marionette.CompositeView.extend({
        template: 'sourceList',
        itemView: SourceView.SourceRow,
        itemViewContainer: 'tbody'
    });

    SourceView.SourcePage = Marionette.Layout.extend({
        template: 'sourcePage',
        events: {
            'click .refreshButton' : 'refreshSources',
            'click .removeSourceLink' : 'removeSource',
            'click .addSourceLink' : 'addSource'
        },
        initialize: function(){
            this.listenTo(wreqr.vent, 'editSource', this.editSource);
            this.listenTo(wreqr.vent, 'refreshSources', this.refreshSources);
            this.listenTo(wreqr.vent, 'changeConfiguration', this.changeConfiguration);
            new SourceView.ModalController({
                application: this
            });
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
            wreqr.vent.trigger("showModal", 
                new ModalSource.View({
                    model: model,
                    parentModel: this.model,
                    mode: 'edit'
                })
            );
        },
        removeSource: function() {
            if(this.model) {
                wreqr.vent.trigger("showModal", 
                    new SourceView.DeleteModal({
                        model: this.model,
                        collection: this.model.get('collection')
                    })
                );
            }
        },
        addSource: function() {
            if(this.model) {
                wreqr.vent.trigger("showModal", 
                    new ModalSource.View({
                        model: this.model.getSourceModelWithServices(),
                        parentModel: this.model,
                        mode: 'add'
                    })
                );
            }
        }
    });

    SourceView.ModalController = Marionette.Controller.extend({
        initialize: function (options) {
            this.application = options.application;
            this.listenTo(wreqr.vent, "showModal", this.showModal);
        },
        showModal: function(modalView) {
            // Global div for workaround with iframe resize and modals
            var region = this.application.getRegion('sourcesModal');
            var iFrameModalDOM = $('#IframeModalDOM');
            modalView.$el.on('hidden.bs.modal', function () {
                iFrameModalDOM.hide();
            });
            modalView.$el.on('shown.bs.modal', function () {
                var modalHeight = (modalView.$el.height() * 1.7) ;
                iFrameModalDOM.height(modalHeight);
                iFrameModalDOM.show();
            });
            region.show(modalView);
            region.currentView.$el.modal();
        }
    });

    SourceView.DeleteItem = Marionette.ItemView.extend({
        template: "deleteSource"
    });

    SourceView.DeleteModal  = Marionette.CompositeView.extend({
        template: 'deleteModal',
        className: 'modal',
        itemView: SourceView.DeleteItem,
        itemViewContainer: '.modal-body',
        events: {
            'click .submit-button' : 'deleteSources'
        },
        deleteSources: function() {
            var view = this;
            var toDelete = [];
            view.collection.each(function (item) {
                var currentConfig = item.get('currentConfiguration');
                var disableConfigs = item.get('disabledConfigurations');
                view.$(".selectSourceDelete").each(function(index, content) {
                    if (content.checked) {

                        var id = currentConfig ? currentConfig.get('id') : null;
                        if (id === content.value) {
                            toDelete.push(view.createDeletePromise(item, currentConfig));
                        } else if (disableConfigs) {
                            disableConfigs.each(function (disabledConfig) {
                                if (disabledConfig.get('id') === content.value) {
                                    toDelete.push(view.createDeletePromise(item, disabledConfig));
                                }
                            });
                        }
                    }
                });
            });

            //remove queued source configurations and entire source config if necessary
            if (toDelete.length > 0) {
                //remove all selected configurations from the current item
                Q.all(toDelete).then(function(results) {
                    _.each(results, function(result) {
                        var item = result.source;
                        if (item.size() <= 0) {
                            //if no type configurations, delete the entire source.
                            view.model.get('collection').removeSource(item);
                            view.model.get('model').get('value').remove(item);
                        }
                    });
                    wreqr.vent.trigger('refreshSources');
                    view.$el.modal("hide");
                });
            }
        },
        createDeletePromise: function(source, config) {
          var deferred = Q.defer();
          var serviceModels = this.model.get('model').get('value');
          config.destroy().success(function() {
              source.removeConfiguration(config);
              //sync up the service model so that the refresh updates properly
              serviceModels.remove(config.getService());
              deferred.resolve({
                  source: source,
                  config: config
              });
          }).fail(function() {
              deferred.reject(new Error("Unable to delete configuration '" + source.get('name') + "'."));
          });
          return deferred.promise;
        }
    });

    return SourceView;

});