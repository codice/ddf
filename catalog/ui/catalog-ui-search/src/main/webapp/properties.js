/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
var DEFAULT_PAGE_SIZE = 25;

define(function (require) {
    'use strict';
    var $ = require('jquery');
    var _ = require('underscore');

    function match(regexList, attribute) {
        return _.chain(regexList)
            .map(function(str) {
                return new RegExp(str);
            })
            .find(function(regex) {
                return regex.exec(attribute);
            }).value() !== undefined;
    }

    var properties = {
        product: 'Catalog',
        canvasThumbnailScaleFactor : 10,
        slidingAnimationDuration : 150,

        defaultFlytoHeight : 15000.0,

        CQL_DATE_FORMAT : 'YYYY-MM-DD[T]HH:mm:ss[Z]',

        ui: {},

        filters: {
            METADATA_CONTENT_TYPE: 'metadata-content-type',
            SOURCE_ID: 'source-id',
            GEO_FIELD_NAME: 'anyGeo',
            ANY_GEO: 'geometry',
            ANY_TEXT: 'anyText',
            OPERATIONS : {
                'string': ['contains', 'matchcase','equals'],
                'xml': ['contains', 'matchcase','equals'],
                'date': ['before','after'],
                'number': ['=','>','>=','<','<='],
                'geometry': ['intersects']
            },
            numberTypes : ['float','short', 'long','double', 'integer']
        },

        init : function(){
            // use this function to initialize variables that rely on others
            var props = this;
            $.ajax({
                async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
                cache: false,
                dataType: 'json',
                url: "/search/catalog/internal/config"
            }).success(function(data) {
                    props = _.extend(props, data);

                $.ajax({
                    async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
                    cache: false,
                    dataType: 'json',
                    url: "/services/platform/config/ui"
                }).success(function(uiConfig){
                    props.ui = uiConfig;
                    return props;
                }).fail(function(jqXHR, status, errorThrown){
                    if(console){
                        console.log('Platform UI Configuration could not be loaded: (status: ' + status + ', message: ' + errorThrown.message + ')');
                    }
                });

            }).fail(function(jqXHR, status, errorThrown) {
                throw new Error('Configuration could not be loaded: (status: ' + status + ', message: ' + errorThrown.message + ')');
            });

            this.handleEditing();
            this.handleFeedback();

            return props;
        },
        handleEditing: function(){
            $('html').toggleClass('is-editing-restricted', this.isEditingRestricted());
        },
        handleFeedback: function(){
            $('html').toggleClass('is-feedback-restricted', this.isFeedbackRestricted());
        },
        isHidden: function(attribute){
          return match(this.hiddenAttributes, attribute);
        },
        isReadOnly: function(attribute){
          return match(this.readOnly, attribute);
        },
        isEditingRestricted: function(){
            return !this.isEditingAllowed;
        },
        getPageSize: function(){
            return this.resultPageSize || DEFAULT_PAGE_SIZE;
        },
        isFeedbackRestricted: function(){
            return !this.queryFeedbackEnabled;
        }
    };

    return properties.init();
});
