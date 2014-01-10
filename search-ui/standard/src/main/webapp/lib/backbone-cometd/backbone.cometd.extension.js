/**
 * Created by tustisos on 12/19/13.
 */
(function (undefined) {
    "use strict";
    var _ = require('underscore'),
        $ = require('jquery'),
        Backbone = require('backbone'),
        Cometd = require('cometdinit');

    var cometdBind = function () {
        var model = this;
        var dataChannel = "/" + this.get("guid");
        var subscription = Cometd.Comet.addListener(dataChannel, function (message) {
            var data = $.parseJSON(message.data);
            //TODO: merge that data obj with "this" one
            model.set(data);
        });
        this.subscription = subscription;
        Cometd.Comet.publish('/service/results/subscribe', { channel: dataChannel});
    };

    Backbone.Model.prototype.cometdBind = cometdBind;
    Backbone.Collection.prototype.cometdBind = cometdBind;

    var cometdUnbind = function () {
        var dataChannel = "/" + this.get("guid");
        Cometd.Comet.removeListener(this.subscription);
        Cometd.Comet.publish('/service/results/unsubscribe', { channel: dataChannel});
    };
    Backbone.Model.prototype.cometdUnbind = cometdUnbind;
    Backbone.Collection.prototype.cometdUnbind = cometdUnbind;

    Backbone.Model.prototype.origDestroy = Backbone.Model.prototype.destroy;
    Backbone.Collection.prototype.origDestroy = Backbone.Collection.prototype.destroy;
    var destroyModel = function (options) {
        Backbone.Model.prototype.cometdUnbind();
        Backbone.Model.prototype.origDestroy(options);
    };
    var destroyColl = function (options) {
        Backbone.Collection.prototype.cometdUnbind();
        Backbone.Collection.prototype.origDestroy(options);
    };
    Backbone.Model.prototype.destroy = destroyModel;
    Backbone.Collection.prototype.destroy = destroyColl;

}(undefined));