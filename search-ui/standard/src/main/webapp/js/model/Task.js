/*global define*/
define([
    'backbone',
    'jquery',
    'underscore'
], function(Backbone, $, _) {
    var Task = {};

    Task.Model = Backbone.Model.extend({
        url: '/service/action',
        useAjaxSync: false,
        validate : function(attrs) {
            if(!attrs.id)
                return "Task must have id.";
            if(!attrs.message)
                return "Task must have message.";
            if(!attrs.timestamp)
                return "Task must have timestamp.";
        },
        parse : function(resp){
            var json = $.parseJSON(resp.data);
            if(json.operations) {
                //remove the stuff that we have pre-defined actions for in the template
                json.operationsExt = this.pluck(json.operations, ['download', 'retry', 'cancel', 'pause', 'remove', 'resume']);
            }
            return json;
        },
        pluck: function(object, items) {
            var newObject = _.clone(object), i;
            for(i = 0; i < items.length; i++) {
                delete newObject[items[i]];
            }
            return newObject;
        }
    });

    Task.Collection = Backbone.Collection.extend({
        model: Task.Model,
        comparator: function(a, b) {
            if(a.get('category') === b.get('category')) {
                return a.get('timestamp') - b.get('timestamp');
            } else {
                return a.get('category').localeCompare(b.get('category'));
            }
        },
        updateTask: function(task) {
            var model = this.findWhere({id:task.get('id')});
            if(model) {
                model.set(task.attributes);
                return model;
            } else {
                this.add(task);
                return task;
            }
        }
    });

    return Task;
});