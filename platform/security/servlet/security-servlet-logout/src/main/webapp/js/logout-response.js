console.log("Beginning logout-response.js");

var searchString = (window.location.search + '').split('?');

if(searchString[1] !== undefined)
{
    var searchParams = searchString[1].split('&');

    searchParams.forEach(function(paramString){

        var param = paramString.split('=');

        if(param[0] === "msg"){
            $('.logout-msg').text(param[1].split('+').join(' '));
        }
        //add additional params here if needed
    });
}
