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
/* global define */
define(['application',
        'cometdinit',
        'js/model/Task',
        'wreqr'],
    function(Application, Cometd, Task,  wreqr) {

        Application.App.module('TaskModule', function(TaskModule) {

            var tasks = new Task.Collection();

            wreqr.reqres.setHandler('tasks', function () {
                return tasks;
            });

            wreqr.vent.trigger('task:update', tasks.at(0));

            TaskModule.addInitializer(function() {

                this.subscription = Cometd.Comet.subscribe("/ddf/activities/**", function(resp) {
                    var task = new Task.Model(resp, {validate: true, parse: true});

                    if(!task.validationError){
                        var model = tasks.updateTask(task);
                        wreqr.vent.trigger('task:update', model);
                    }
                });
            });
        });

    });