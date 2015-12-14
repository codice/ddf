//- Alert Hellllllo world?
//- Get logout actions /services/logout/actions
//- Render UI
//    - Buttons based on logout actions
//    - Display home and go back buttons
//- Setup events for when user clicks them butts
//- Load url of button to iFrame
//- Auto logout if only signed in one place


$(function () {
    $('#go-back').click(function(){
        window.history.back();
    });
});

$.get("/services/logout/actions", function (data) {
    console.log(data);

    var actions = JSON.parse(data);
    if (actions.length === 0) {
        $('<h2>').text('You are not currently logged in.').appendTo('#logouts');
    }
    var logoutDivs = actions.map(function (action) {
        var div = $('<div>').addClass('row');
        
        $('<div>')
            .addClass('col-md-4')
            .addClass('med-font')
            .text('Logged in as ' + action.auth + ' in ' + action.realm)
            .appendTo(div);

        $('<div>')
            .addClass('col-md-4')
            .text('Description: ' + action.description)
            .appendTo(div);


        var button = $('<button>').text('Logout')
            .addClass('btn btn-primary float-right')
            .click(function () {
                $('<iframe>').attr('src', action.url).addClass('iframe-fill').appendTo(div);
                button.attr('disabled', true);
            });

        $('<div>')
            .addClass('col-md-4 text-right')
            .append(button)
            .appendTo(div);

        return div;
    });

    $('#logouts').append(logoutDivs);

    //TODO: Uncomment
    /*if (logoutDivs.length === 1) {
        logoutDivs[0].find('button').click();
    }*/
});
