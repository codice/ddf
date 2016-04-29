var getLatestStableVersion = function(callback) {
    var versionContent = $("#stable-version");

    $.ajax({
        url: "https://api.github.com/repos/codice/ddf/releases/latest",
        success: function(data) {
            var dateStr = data.published_at.slice(0, 10);
            var html = "<a href='" + data.html_url + "' target='_blank'>" + data.tag_name + "</a>" +
                " released on " + dateStr;
            versionContent.html(html);
            
            if (callback !== undefined) {
                callback(data);
            }
        },
        error: function() {
            versionContent.html("<a href='https://github.com/codice/ddf/releases/latest' target='_blank'>Link to the latest release</a>");
            
            if (callback !== undefined) {
                callback();
            }
        },
        dataType: "json"
    });
};

var updateDownloadButton = function(data) {
    var downloadButton = $("#download-button"), href, html;
    
    if (data !== undefined) {
        href = data.assets[0].browser_download_url;
        html = "Download .zip <span class='glyphicon glyphicon-download'></span>";
    } else {
        href = "https://github.com/codice/ddf/releases/latest";
        html = "Download from GitHub";
    }
    
    downloadButton.attr("href", href);
    downloadButton.html(html);
};

$(function() {
    if (window.location.href.indexOf("Downloads") > -1) { // We're on the downloads page.
        getLatestStableVersion(updateDownloadButton);
    } else {
        getLatestStableVersion();
    }
});

$("#hamburger").on("click", function (){
    console.log("happening");
    $("#navbar").toggleClass("small-hidden");
});