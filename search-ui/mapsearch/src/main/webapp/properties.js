/*global define*/

define(function () {
    'use strict';

    var properties = {

        $: require('jquery'),
        canvasThumbnailScaleFactor : 10,
        slidingAnimationDuration : 150,

        defaultFlytoHeight : 15000.0,

        init : function(){
            // use this function to initialize variables that rely on others
            var props = this;
            this.$.ajax({
                async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
                cache: false,
                dataType: 'json',
                url: "/services/store/config"
            }).success(function(data) {
                    props.footer = data.footer;
                    props.style = data.style;
                    props.background = data.background;
                    props.header = data.header;
                    props.branding = data.branding;
                    return props;
                }).fail(function(jqXHR, status, errorThrown) {
                    throw Error('Configuration could not be loaded: (status: ' + status + ', message: ' + errorThrown.message + ')');
                });

            return props;
        }
    };

    return properties.init();
});
