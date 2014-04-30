/*global define*/

define(["backbone", "application", "underscore", "jquery"], function(Backbone, App, _, $) {
    
    var Notification = App.module();

    
    Notification.Notification = Backbone.Model.extend({
        initialize : function(){
            this.set("timestamp", parseInt(this.get("timestamp"), 10));
        },
        
        //validates the notification ensuring it contains the 3 necessary parts
        validate : function(attrs) {
            if(!attrs.application)
                return "Notification must have application.";
            if(!attrs.title)
                return "Notification must have title.";
            if(!attrs.message)
                return "Notification must have message.";
            if(!attrs.timestamp)
                return "Notification must have timestamp.";
        },
        
        //parses out the object returned from CometD 
        parse : function(resp){
            return $.parseJSON(resp.data);
        }
    });

    var addOptions = {add: true, remove: false};
    Notification.List = Backbone.Collection.extend({
        model: Notification.Notification,
        comparator: 'timestamp',
        policies: {
            latest: function(timeCutOff) {
                var coll = this;
                this.each(function(notification) {
                    if(notification.get('timestamp') + timeCutOff < coll.now) {
                        coll.remove(notification);
                    }
                });
            },
            last: function(numOfNotifications) {
                var coll = this;
                if(this.length > numOfNotifications) {
                    var num = this.length - numOfNotifications,
                        i = 0;
                    for(;i<num;i++) {
                        coll.shift();
                    }
                }
            }
        },
        policy: false,
        add: function(models, options) {
            var setRet = this.set(models, _.extend({merge: false}, options, addOptions));
            this.now = $.now();
            if(this.policy) {
                var func = this.policies[this.policy];
                if(typeof func === 'function') {
                    func.call(this, this.policyArg);
                }
            }
            return setRet;
        }
    }); 
    
    return Notification; 
});

