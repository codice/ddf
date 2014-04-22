/*global define*/

define(["backbone", "application", "moment", "jquery"], function(Backbone, App, moment, $) {
    
    var Notification = App.module();

    
    Notification.Notification = Backbone.Model.extend({
        initialize : function(){
            this.set("timestamp", parseInt(this.get("timestamp"), 10));
            var formattedTime = moment(this.get("timestamp")).format('MMMM Do YYYY, h:mm:ss a');
            this.set("formattedTime", formattedTime);
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
    
    Notification.List = Backbone.Collection.extend({
        model: Notification.Notification,
        comparator: 'timestamp'
    }); 
    
    return Notification; 
});

