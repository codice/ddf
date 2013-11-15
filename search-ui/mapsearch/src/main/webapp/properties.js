/*global define*/

define(function () {
    'use strict';

    var properties = {


        canvasThumbnailScaleFactor : 10,

        defaultFlytoHeight : 15000.0,

        classification : {
            // Possible values are ['unclassified', 'confidential', 'secret', 'topsecret']
            style : 'unclassified',
            // This will be the text that shows up in the bar.  Allows for us to set caveats / other portions of the classification rather than just the level
            text : 'UNCLASSIFIED'
        },

        init : function(){

            // use this function to initialize variables that rely on others
            return this;
        }
    };

    return properties.init();
});
