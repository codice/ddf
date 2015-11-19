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

    var logoutDivs = actions.map(function (action) {
        var div = $('<div>');
        var iFrame = $('<iframe>').appendTo(div);
        var button = $('<button>')
            .html(action.title)
            .attr('title', action.description)
            .click(function () {
                iFrame.attr('src', action.url).appendTo(div);
                button.remove();
            });
        return div;
    });

    $('#logouts').append(logoutDivs);

    $('#logouts').append(test);

    /*if (logoutDivs.length === 1) {
        logoutDivs[0].find('button').click();
    }*/
});
