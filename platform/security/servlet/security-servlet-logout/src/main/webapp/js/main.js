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
        var div = $('<div class="logout-div">');
        $('<h2>').text('Logged in as ' + action.auth + " in " + action.realm).appendTo(div);
        $('<hr>').appendTo(div);
        var iFrame = $('<iframe>').appendTo(div);
       $('<p>').text('Description: ' + action.description).appendTo(div);
        var button = $('<button class="btn logout-btn">')
            .html(action.title)
            .attr('title', action.description)
            .click(function () {
                iFrame.attr('src', action.url).appendTo(div);
                button.remove();
            });
         div.append(button);
         $('#logouts').append(div);
    });

    /*if (logoutDivs.length === 1) {
        logoutDivs[0].find('button').click();
    }*/
});
