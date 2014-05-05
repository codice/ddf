/* global define */
define(["application",
        "cometdinit",
        "js/model/Task",
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