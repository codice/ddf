/*global define*/

define(function (require) {
    'use strict';
    require('purl');
    var $ = require('jquery');

    var properties = {
        canvasThumbnailScaleFactor : 10,
        slidingAnimationDuration : 150,

        defaultFlytoHeight : 15000.0,

        init : function(){
            // use this function to initialize variables that rely on others
            var props = this;
            $.ajax({
                async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
                cache: false,
                dataType: 'json',
                url: "/services/store/config"
            }).success(function(data) {
                    props.footer = data.footer;
                    props.style = data.style;
                    props.textColor = data.textColor;
                    props.background = data.background;
                    props.header = data.header;
                    props.branding = data.branding;
                    props.layers = data.layers;
                    props.enableWmsServer = data.enableWmsServer;
                    props.format = data.format;
                    props.sync = data.sync;

                    var sync = $.url().param('sync');
                    if (sync === 'true') {
                        props.sync = true;
                    } else if (sync === 'false') {
                        props.sync = false;
                    }

                    return props;
                }).fail(function(jqXHR, status, errorThrown) {
                    throw new Error('Configuration could not be loaded: (status: ' + status + ', message: ' + errorThrown.message + ')');
                });

            return props;
        }
    };

    return properties.init();
});
