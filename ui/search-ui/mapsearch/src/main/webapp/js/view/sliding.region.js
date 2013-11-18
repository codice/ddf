/*global define*/

define(function(require){
    "use strict";
    var Marionette = require('marionette');

    var Region = Marionette.Region.extend({
       open : function(view){
           // src  example
//           this.$el.empty().append(view.el);
        this.$el.hide();
        this.$el.html(view.el);
        this.$el.slideDown("fast");
    }
    });

    return Region;
});
