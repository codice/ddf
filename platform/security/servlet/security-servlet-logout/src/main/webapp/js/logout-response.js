/*global $, window */
(function () {
    var searchString = (window.location.search + '').split('?');

    if (searchString[1] !== undefined) {
        var searchParams = searchString[1].split('&');

        searchParams.forEach(function (paramString) {

            var param = paramString.split('=');

            if (param[0] === "msg") {
                $('#extramessage').html(decodeURIComponent(param[1].split('+').join(' ')));
            }
            //add additional params here if needed
        });
    }
    $('#landinglink').click(function () {
        window.parent.postMessage('landing', window.location.origin + '/logout');
    });
    $('#signinlink').click(function () {
        window.parent.postMessage('signin', window.location.origin + '/logout');
    });
}());