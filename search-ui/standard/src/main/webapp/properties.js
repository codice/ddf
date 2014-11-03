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

define(function (require) {
    'use strict';
    require('purl');
    var $ = require('jquery');
    var _ = require('underscore');

    var properties = {
        canvasThumbnailScaleFactor : 10,
        slidingAnimationDuration : 150,

        defaultFlytoHeight : 15000.0,

        CQL_DATE_FORMAT : 'YYYY-MM-DD[T]HH:mm:ss[Z]',

        filters: {
            METADATA_CONTENT_TYPE: 'metadata-content-type',
            SOURCE_ID: 'source-id',
            ANY_GEO: 'anyGeo',
            ANY_TEXT: 'anyText',
            OPERATIONS : {
                'string': ['contains','equals'],
                'xml': ['contains','equals'],
                'date': ['before','after'],
                'number': ['=','!=','>','>=','<','<='],
                'anyGeo': ['intersects']
            }
        },

        init : function(){
            // use this function to initialize variables that rely on others
            var props = this;
            $.ajax({
                async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
                cache: false,
                dataType: 'json',
                url: "/services/store/config"
            }).success(function(data) {
                    props = _.extend(props, data);
                    return props;
                }).fail(function(jqXHR, status, errorThrown) {
                    throw new Error('Configuration could not be loaded: (status: ' + status + ', message: ' + errorThrown.message + ')');
                });

            return props;
        }
    };

    return properties.init();
});
