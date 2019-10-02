var getLatestStableVersion = function(callback) {
    var versionContent = $("#stable-version");

    $.ajax({
        url: "https://api.github.com/repos/codice/ddf/releases",
        success: function(data) {

            const currentVersion = data.reduce(
              (maxElement, curElement) =>
                curElement.tag_name > maxElement.tag_name ? curElement : maxElement,
              { tag_name: '0' }
            )

            var dateStr = currentVersion.published_at.slice(0, 10);
            var html = "<a href='" + currentVersion.html_url + "' target='_blank'>" + currentVersion.tag_name.toUpperCase() + "</a>" +
                " released on " + dateStr;
            versionContent.html(html);
            
            var downloadButton = $("#download-button");
            var buttonHref = currentVersion.html_url
            var buttonHtml = "Download .zip <span class='glyphicon glyphicon-download'></span>";

            downloadButton.attr("href", buttonHref);
            downloadButton.html(buttonHtml);

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

var setNewsAndEvents = function(data) {
    var versionHistory = $("#version-history");
    
    if (data !== undefined) {
        data = data.reverse()
        data.forEach(element => {

            var href = element.html_url
            var dateString = element.published_at.slice(0, 10)
            var versionName = element.name

            html = `<a class='list-group-item' href='${href}'>\n<h4 class='list-group-item-heading'>${dateString}</h4>\n<p class'list-group-item-text'>The DDF Development Team is pleased to announce the availability of ${versionName}</p></a>`

            versionHistory.prepend(html)
        });
    }
};

var setPreviousVersionList = function(data) {
    var versionHistoryTable = $("#version-history-table"),versionName,dateString,href;
    if (data !== undefined) {
        data.reverse()
        data.forEach(element => {
            href = element.html_url
            dateString = element.published_at.slice(0, 10)
            versionName = element.name.slice(4)

            html = `<tr>\n<td>${versionName}</td>\n<td>${dateString}</td>\n<td class='text-center'><a href='http://artifacts.codice.org//service/local/artifact/maven/redirect?g=ddf&a=docs&v=${versionName}&r=public&c=documentation&e=html' target='_blank'>html</a></td>\n<td class='text-center'><a href='http://artifacts.codice.org//service/local/artifact/maven/content?g=ddf&a=docs&v=${versionName}&r=public&c=documentation&e=pdf' target='_blank'>pdf</a></td>\n</tr>`

            versionHistoryTable.prepend(html);
        });
    }
};
  

$(function() {
    if (window.location.href.indexOf("Documentation-versions") > -1 ) {
        getLatestStableVersion(setPreviousVersionList);
    } else {
        getLatestStableVersion(setNewsAndEvents);
    }
});

$("#hamburger").on("click", function (){
    $("#navbar").toggleClass("small-hidden");
});