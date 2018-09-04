/*global $, window, decodeURI */
(function () {
    var prevUrl = $.url().param('prevurl');

    $.get("../services/platform/config/ui", function (data) {
        $('.nav img').attr('src', "data:image/png;base64," + data.productImage);
        $('.nav label').attr('title', data.version);
        $('.nav label:first-of-type').append(data.version);
        $('.nav label button').click(function () {
            window.location.href = window.location.origin + ((prevUrl !== undefined && prevUrl !== 'undefined') ? decodeURI(prevUrl) : '');
        });
    });

    $.get("../services/logout/actions", function (data) {

        var actions = JSON.parse(data);

        var doLogout = function (action) {
            $('iframe').attr('src', action.url);
            $('#modal').removeClass('is-hidden');
        };

        if (actions.length !== 0) {
            $('#noActions').toggleClass('is-hidden', actions.length !== 0);
            $('#actions').toggleClass('is-hidden', actions.length === 0);
            actions.forEach(function (action) {
                if ($.url().param('noPrompt')) {
                    doLogout(action);
                } else {
                    var $row = $('<tr></tr>');
                    var $realm = $('<td></td>');
                    $realm.html(action.realm);
                    var $realmDescription = $('<a href="#" class="description" tabindex="-1"></a>');
                    $realmDescription.data('title', action.description);
                    $realmDescription.data('placement', 'right');
                    $realmDescription.append($('<i class="glyphicon glyphicon-question-sign"></i>'));
                    $realm.append($realmDescription);
                    var $user = $('<td></td>');
                    $user.html(action.auth);
                    var $logout = $('<td></td>');
                    var $button = $('<button>').attr('id', 'logoutButton')
                        .text('Logout')
                        .addClass('btn btn-primary float-right')
                        .click(function () {
                            doLogout(action);
                        });
                    $logout.append($button);
                    $row.append($realm).append($user).append($logout);
                    $('#actions').find('tbody').append($row);
                }
            });

            $('.description').tooltip();

        }
    });

    var handleIframeResponse = function (e) {
        if (e.origin === window.location.origin) {
            var action = e.data.split(':')[0];
            if (action === 'landing') {
                window.location.href = window.location.origin;
            } else if (action === 'signin') {
                window.location.href = window.location.origin + '/login' + ((prevUrl !== undefined && prevUrl !== 'undefined') ? '?prevurl=' + prevUrl : '');
            }
        }
    };
    window.addEventListener('message', handleIframeResponse, false);
    $('#landinglink').click(function () {
        window.location.href = window.location.origin;
    });
    $('#signinlink').click(function () {
        window.location.href = window.location.origin + '/login' + ((prevUrl !== undefined && prevUrl !== 'undefined') ? '?prevurl=' + prevUrl : '');
    });
}());

