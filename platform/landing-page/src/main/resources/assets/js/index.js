(function () {

    var getSourceAvailabilities = function () {
        var sourceAvailabilityList = $("#sourceAvailabilityList");
        sourceAvailabilityList.hide();

        var spinner = $("#spinner");
        spinner.spin({
            lines: 13, // The number of lines to draw
            length: 12, // The length of each line
            width: 10, // The line thickness
            radius: 25, // The radius of the inner circle
            corners: 1, // Corner roundness (0..1)
            rotate: 0, // The rotation offset
            direction: 1, // 1: clockwise, -1: counterclockwise
            color: '#929292', // #rgb or #rrggbb or array of colors
            speed: 1, // Rounds per second
            trail: 60, // Afterglow percentage
            shadow: false, // Whether to render a shadow
            hwaccel: false, // Whether to use hardware acceleration
            zIndex: 2e9 // The z-index (defaults to 2000000000)
        });

        $.ajax({
            url: "./search/catalog/internal/catalog/sources",
            success: function (data) {
                if (data.length > 0) {
                    data.sort(function (a, b) {
                        if (a.id < b.id)
                            return -1;
                        else if (a.id > b.id)
                            return 1;
                        else
                            return 0;
                    });
                    $.each(data, function (index, value) {
                        var listItem = "";
                        if (value.available) {
                            listItem = "<li class='list-group-item list-group-item-success'>";
                        } else {
                            listItem = "<li class='list-group-item list-group-item-danger'>";
                        }
                        listItem += value.id + " " + value.version;
                        listItem += "</li>";
                        sourceAvailabilityList.append(listItem);
                    });
                } else {
                    sourceAvailabilityList.html("<li class='list-group-item list-group-item-info'>No sources found</li>");
                }
                // Stop spinner.
                spinner.spin(false);
                spinner.hide();

                sourceAvailabilityList.show();
            },
            error: function () {
                // Stop spinner.
                spinner.spin(false);
                spinner.hide();

                sourceAvailabilityList.html("<li class='list-group-item list-group-item-warning'>Error locating sources</li>");
                sourceAvailabilityList.show();
            }
        });
    };

    var getHeaderFooter = function () {
        var header = $("#header");
        var footer = $("#footer");
        var content = $("#content");
        $.ajax({
            url: "./services/platform/config/ui",
            getBanner: function (data, text) {
                return "<div style='color: " + data.color + "; background-color:" + data.background + "'>" + $('<div/>').text(text).html() + "</div>";
            }, success: function (data) {
                var headerText = data.header;
                if (headerText) {
                    header.html(this.getBanner(data, headerText));
                    content.css("top", 20);
                    header.show();
                } else {
                    header.height(0);
                }
                var footerText = data.footer;
                if (footerText) {
                    footer.html(this.getBanner(data, footerText));
                    content.css("bottom", 20);
                    footer.show();
                } else {
                    footer.height(0);
                }
            }
        });
    };

    getSourceAvailabilities();
    getHeaderFooter();
})();

