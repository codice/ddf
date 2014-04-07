/* global define */

define(['backbone', 'js/model/Notification'], function(Backbone, Notification){
    var NotificationList = Backbone.Collection.extend({
        model: Notification,
        comparator: 'timestamp'
        
        // this will need to be removed and we would utilize the backbone.cometd.extension.js to provide the correct syncing 
        //localStorage : new LocalStorage("notifications-backbone-storage")   
    
        //,url: <URL to external notifications API>
        //,useAjaxSync: false,  //this is in order to bypass the normal Backbone Sync capabilities.
    }); 
    
    return NotificationList;
});