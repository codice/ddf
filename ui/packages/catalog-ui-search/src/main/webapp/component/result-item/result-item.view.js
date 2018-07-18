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
import React from 'react';
const Backbone = require('backbone');
const Marionette = require('marionette');
const _ = require('underscore');
const $ = require('jquery');
const CustomElements = require('js/CustomElements');
const IconHelper = require('js/IconHelper');
const store = require('js/store');
const Common = require('js/Common');
const DropdownModel = require('component/dropdown/dropdown');
const MetacardInteractionsView = require('component/metacard-interactions/metacard-interactions.view');
const ResultIndicatorView = require('component/result-indicator/result-indicator.view');
const properties = require('properties');
const router = require('component/router/router');
const user = require('component/singletons/user-instance');
const metacardDefinitions = require('component/singletons/metacard-definitions');
const moment = require('moment');
const sources = require('component/singletons/sources-instance');
const HoverPreviewDropdown = require('component/dropdown/hover-preview/dropdown.hover-preview.view');
const ResultAddView = require('component/result-add/result-add.view');
const PopoutView = require('component/dropdown/popout/dropdown.popout.view');
require('behaviors/button.behavior');
require('behaviors/dropdown.behavior');
const HandleBarsHelpers = require('js/HandlebarsHelpers');

module.exports = Marionette.LayoutView.extend({
    template(data) {
        return (
            <React.Fragment>
                <div className="result-container" data-metacard-id={data.id}
                data-query-id={data.metacard.queryId}>
                <div className="container-indicator">
                </div>
                <div className="container-content">
                    <div className="content-header">
                        <span className="header-icon fa fa-history" title="Type: Revision" data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"></span>
                        <span className="header-icon fa fa-book" title="Type: Workspace" data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"></span>
                        <span className="header-icon fa fa-file" title="Type: Resource" data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"></span>
                        <span className="header-icon fa fa-trash" title="Type: Deleted" data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"></span>
            
                        <span className={`header-icon result-icon ${data.icon}`} title="Type: Resource" data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"></span>
                        <span className="header-title" data-help={HandleBarsHelpers.getAlias('title')} title={`${HandleBarsHelpers.getAlias('title')}: ${data.metacard.properties.title}`}>
                            {data.metacard.properties.title}
                        </span>
                    </div>
                    <div className="content-body">
                        {
                            data.metacard.properties.thumbnail ? 
                            <div className="detail-thumbnail details-property" data-help={HandleBarsHelpers.getAlias('thumbnail')}>
                            </div> : ''
                        }
                        {
                            data.customDetail.map((detail) => {
                                return (<div key={detail.label} className="detail-custom details-property" data-help={HandleBarsHelpers.getAlias(detail.label)} title={`${HandleBarsHelpers.getAlias(detail.label)}: ${detail.value}`}>
                                <span>
                                    {detail.value}
                                    </span>
                                </div>);
                            })
                        }
                        {
                            data.showRelevanceScore ? 
                            <div className="detail-custom details-property" data-help={`Relevance: ${data.relevance}`} title={`Relevance: ${data.relevance}`}>
                                <span>
                                {data.roundedRelevance}
                                </span>
                            </div> : ''
                        }
                        {
                            data.showSource ? 
                            <div className="detail-source details-property" title={`${HandleBarsHelpers.getAlias('source-id')}: ${data.metacard.properties['source-id']}`} 
                                data-help={HandleBarsHelpers.getAlias('source-id')}>
                                {
                                    data.local ? 
                                    <React.Fragment>
                                        <span className="fa source-icon fa-home">
                                        </span>
                                        <span>local</span>
                                    </React.Fragment> : 
                                    <React.Fragment>
                                        <span className="fa source-icon fa-cloud">
                                        </span>
                                        <span>
                                            {data.metacard.properties['source-id']}
                                        </span>
                                    </React.Fragment>
                                }
                            </div> : ''
                        }
                    </div>
                    <div className="content-footer">
                        <div className="result-validation">
                            {
                                data.hasError ? 
                                <span className="fa fa-exclamation-triangle resultError" title="Has validation errors." data-help="Indicates the given result has a validation error.
                                See the 'Quality' tab of the result for more details.">
                                </span> : ''
                            }
                            {
                                !data.hasError && data.hasWarning ?
                                <span className="fa fa-exclamation-triangle resultWarning" title="Has validation warnings." data-help="Indicates the given result has a validation warning.
                                See the 'Quality' tab of the result for more details.">
                                </span> : ''
                            }
                        </div>
                        <button className="result-download fa fa-download is-button is-neutral" title="Downloads the results associated product directly to your machine." data-help="Downloads
                        the results associated product directly to your machine.">
                        </button>
                        <div class="result-link is-button is-neutral composed-button" title="Follow external links" data-help="Follow external links.">
                        </div>
                        <button className="result-add is-button is-neutral composed-button" title="Add or remove the result from a list, or make a new list with this result." data-help="Add or remove the result from a list, or make a new list with this result.">
                            <span className="fa fa-plus"></span>
                        </button>
                        <button className="result-actions is-button" title="Provides a list of actions to take on the result." data-help="Provides a list
                        of actions to take on the result.">
                            <span className="fa fa-ellipsis-v"></span>
                        </button>
                    </div>
                </div>
            </div>
        </React.Fragment>
        )   
    },
    attributes: function(){
        return {
            'data-resultid': this.model.id
        };
    },
    tagName: CustomElements.register('result-item'),
    modelEvents: {
    },
    events: {
        'click .result-download': 'triggerDownload'
    },
    regions: {
        resultActions: '.result-actions',
        resultIndicator: '.container-indicator',
        resultThumbnail: '.detail-thumbnail',
        resultAdd: '.result-add'
    },
    behaviors() {
        return {
            button: {},
            dropdown: {
                dropdowns: [
                    {
                        selector: '.result-actions',
                        view: MetacardInteractionsView.extend({
                            behaviors: {
                                navigation: {}
                            }  
                        }),
                        viewOptions: {
                            model: new Backbone.Collection([this.options.model])
                        }
                    },
                    {
                        selector: '.result-add',
                        view: ResultAddView,
                        viewOptions: {
                            model: new Backbone.Collection([this.options.model])
                        }
                    }
                ]
            }
        };   
    },
    initialize: function(options){
        if (!options.selectionInterface) {
            throw 'Selection interface has not been provided';
        }
        this.checkDisplayType();
        this.checkTags();
        this.checkIsInWorkspace();
        this.checkIfDownloadable();
        this.checkIfBlacklisted();
        this.listenTo(this.model, 'change:metacard>properties change:metacard', this.handleMetacardUpdate);
        this.listenTo(user.get('user').get('preferences'), 'change:resultDisplay', this.checkDisplayType);
        this.listenTo(user.get('user').get('preferences').get('resultBlacklist'),
            'add remove update reset', this.checkIfBlacklisted);
        this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update add remove reset', this.handleSelectionChange);
        this.handleSelectionChange();
    },
    handleSelectionChange: function() {
        var selectedResults = this.options.selectionInterface.getSelectedResults();
        var isSelected = selectedResults.get(this.model.id);
        this.$el.toggleClass('is-selected', Boolean(isSelected));
    },
    handleMetacardUpdate: function(){
        this.$el.attr(this.attributes());
        this.render();
        this.onBeforeShow();
        this.checkDisplayType();
        this.checkTags();
        this.checkIsInWorkspace();
        this.checkIfBlacklisted();
        this.checkIfDownloadable();
    },
    onBeforeShow: function(){
        this.resultIndicator.show(new ResultIndicatorView({
            model: this.model
        }));
        this.handleResultThumbnail();
    },
    handleResultThumbnail: function() {
        if (this.model.get('metacard').get('properties').get('thumbnail')) {
            this.resultThumbnail.show(new HoverPreviewDropdown({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
        }
    },
    addConfiguredResultProperties: function(result){
        result.showSource = false;
        result.customDetail = [];
        if (properties.resultShow) {
            properties.resultShow.forEach(function (additionProperty) {
                if (additionProperty === 'source-id') {
                    result.showSource = true;
                    return;
                }
                var value = result.metacard.properties[additionProperty];
                if (value && metacardDefinitions.metacardTypes[additionProperty]) {
                    switch (metacardDefinitions.metacardTypes[additionProperty].type) {
                        case 'DATE':
                            if (value.constructor === Array) {
                                value = value.map(function (val) {
                                    return Common.getMomentDate(val);
                                });
                            } else {
                                value = Common.getMomentDate(value);
                            }
                            break;
                    }
                    result.customDetail.push({
                        label: additionProperty,
                        value: value
                    });
                }
            });
        }
        return result;
    },
    massageResult: function(result){
        //make a nice date
        result.local = Boolean(result.metacard.properties['source-id'] === sources.localCatalog);
        var dateModified = moment(result.metacard.properties.modified);
        result.niceDiff = Common.getMomentDate(dateModified);

        //icon
        result.icon = IconHelper.getClass(this.model);

        //check validation errors
        var validationErrors = result.metacard.properties['validation-errors'];
        var validationWarnings = result.metacard.properties['validation-warnings'];
        if (validationErrors){
            result.hasError = true;
            result.error = validationErrors;
        }
        if (validationWarnings){
            result.hasWarning = true;
            result.warning = validationWarnings;
        }

        //relevance score
        result.showRelevanceScore = properties.showRelevanceScores && result.relevance !== null;
        if (result.showRelevanceScore === true) {
            result.roundedRelevance = parseFloat(result.relevance).toPrecision(properties.relevancePrecision);
        }

        return result;
    },
    serializeData: function(){
        return this.addConfiguredResultProperties(this.massageResult(this.model.toJSON()));
    },
    checkIfBlacklisted: function(){
        var pref = user.get('user').get('preferences');
        var blacklist = pref.get('resultBlacklist');
        var id = this.model.get('metacard').get('properties').get('id');
        var isBlacklisted = blacklist.get(id) !== undefined;
        this.$el.toggleClass('is-blacklisted', isBlacklisted);
    },
    checkIsInWorkspace: function(){
        var currentWorkspace = store.getCurrentWorkspace();
        this.$el.toggleClass('in-workspace', Boolean(currentWorkspace));
    },
    checkIfDownloadable: function() {
        this.$el.toggleClass('is-downloadable', this.model.get('metacard').get('properties').get('resource-download-url') !== undefined);
    },
    checkDisplayType: function() {
        var displayType = user.get('user').get('preferences').get('resultDisplay');
        switch(displayType){
            case 'List':
                this.$el.removeClass('is-grid').addClass('is-list');
                break;
            case 'Grid':
                this.$el.addClass('is-grid').removeClass('is-list');
                break;
        }
    },
    checkTags: function(){
        this.$el.toggleClass('is-workspace', this.model.isWorkspace());
        this.$el.toggleClass('is-resource', this.model.isResource());
        this.$el.toggleClass('is-revision', this.model.isRevision());
        this.$el.toggleClass('is-deleted', this.model.isDeleted());
        this.$el.toggleClass('is-remote', this.model.isRemote());
    },
    triggerDownload: function(e) {
        window.open(this.model.get('metacard').get('properties').get('resource-download-url'));
    }
});