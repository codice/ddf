$('#modal').on('click',function(){
    $(this).addClass('is-hidden');
                    window.top.location.reload();

});

$.get("/services/platform/config/ui", function(data){
    $('#nav img').attr('src', "data:image/png;base64,"+data.productImage);
});

$.get("/services/logout/actions", function (data){

    var actions = JSON.parse(data);

    if(actions.length === 0) {
        $("#actions").replaceWith($('iframe').attr('src', '/logout/logout-response.html?msg=You+are+not+logged+in'));
    }
    else {
        $('#noActions').toggleClass('is-hidden', actions.length!==0);
        $('#actions').toggleClass('is-hidden', actions.length===0);


        actions.forEach(function(action){
            var $row = $('<tr></tr>');
            var $realm = $('<td></td>');
            $realm.html(action.realm);
            var $realmDescription = $('<a href="#" class="description" tabindex="-1"></a>');
            $realmDescription.data('title',action.description);
            $realmDescription.data('placement','right');
            $realmDescription.append($('<i class="glyphicon glyphicon-question-sign"></i>'));
            $realm.append($realmDescription);
            var $user = $('<td></td>');
            $user.html(action.auth);
            var $logout = $('<td></td>');
            var $button = $('<button>').text('Logout')
                                      .addClass('btn btn-primary float-right')
                                      .click(function () {
                                            $('iframe').attr('src',action.url);
                                            $('#modal').removeClass('is-hidden');

                                      });
            $logout.append($button);
            $row.append($realm).append($user).append($logout);
            $('#actions tbody').append($row);
        });

                    $('.description').tooltip();

    }
});


