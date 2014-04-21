/*global define*/

define(['jquery'], function ($) {
    'use strict';

    var user = {
        init : function(){
            var user = this;
            $.ajax({
                async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
                cache: false,
                dataType: 'json',
                url: '/search/standard/user'
            }).success(function(data) {
                user = data.user;

                return user;
            }).fail(function(jqXHR, status, errorThrown) {
                throw new Error('User could not be loaded: (status: ' + status + ', message: ' +
                    errorThrown.message + ')');
            });

            return user;
        }
    };

    return user.init();
});
