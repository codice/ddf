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
define([
    'backbone',
    'underscore',
    'moment'
], function(Backbone, _, moment) {
    var Task = {};

    Task.Model = Backbone.Model.extend({
        url: '/service/action',
        useAjaxSync: false,
        initialize : function() {
            this.set("timestamp", moment(this.get("timestamp")));

            var operations = this.get("operations");
            // Extract any operations we don't have predefined actions for in the template.
            var operationsExt = _.omit(operations, 'download', 'retry', 'cancel', 'pause', 'remove', 'resume');
            this.set("operationsExt", operationsExt);
        },
        validate : function(attrs) {
            if(!attrs.id)
                return "Task must have id.";
            if(!attrs.message)
                return "Task must have message.";
            if(!attrs.timestamp)
                return "Task must have timestamp.";
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