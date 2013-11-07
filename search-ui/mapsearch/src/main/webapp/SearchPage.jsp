<!DOCTYPE html>
<!--
/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
 -->

<html>
<head>
    <meta charset="utf-8">


    <title>DDF Search</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <script src="Cesium/Cesium.js"></script>
    <style>
        @import url(Cesium/Widgets/CesiumWidget/CesiumWidget.css);
        @import url(lib/cesium/bucket.css);

        #toolbar {
            opacity: 0.85;
        }

        .ui-dialog {
            opacity: 0.9;
        }

        .ui-icon-blank {
            background-position: -224px -192px;
        }
    </style>


    <link href="lib/bootstrap-2.3.1/css/bootstrap.min.css" rel="stylesheet">
    <link href="lib/font-awesome/css/font-awesome.min.css" rel="stylesheet">

    <link href="lib/jquery/css/smoothness/jquery-ui-1.9.1.custom.min.css"
          rel="stylesheet">
    <link href="lib/jquery/css/plugin/jquery-ui-timepicker-addon.css"
          rel="stylesheet">

    <!-- These CSS files have been compressed and aggregated into Search-min.css.  The list is here for easy modification for
         the sake of debugging.
         TODO: Leverage something like http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/  for a better solution
     -->
    <!--
    <link href="css/searchPage.css" rel="stylesheet">
    <link href="css/recordView.css" rel="stylesheet">
    -->
    <link href="css/Search-min.css" rel="stylesheet">

    <style media=screen type="text/css">
    .banner {
        color: <%=org.codice.ddf.ui.searchui.search.properties.ConfigurationStore
                            .getInstance().getColor()%>;
        background: <%=org.codice.ddf.ui.searchui.search.properties.ConfigurationStore
                            .getInstance().getBackground()%>;
    }
    </style>


    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/search/shim/html5.js"></script>
    <![endif]-->
    <!--[if IE 7]>
    <link rel="stylesheet" href=/search/font-awesome/css/font-awesome-ie7.min.css">
    <![endif]-->

</head>
<body>

    <div class="navbar navbar-inverse navbar-fixed-top">
        <%
        String h = org.codice.ddf.ui.searchui.search.properties.ConfigurationStore
        .getInstance().getHeader();
        if (h != null && h.trim().length() > 0)
        out.println("<div class=\"banner\">" + h + "</div>");
        %>

        <div class="navbar-inner">
            <a class="brand" href="#"><i style="padding-left: 25px" class="icon-globe icon-white"></i>DDF</a>

            <div class="nav-collapse collapse">
                <ul class="nav">
                    <li class="active"><a href="#">Search</a></li>
                </ul>
                <ul class="nav pull-right">
                    <li><a href="SearchHelp.html?title=DDF">Help</a></li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Metacard Modal -->
    <div id="metacardModal" class="modal hide" tabindex="-1" role="dialog"
         aria-labelledby="myModalLabel" aria-hidden="true">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal"
                    aria-hidden="true">&times;</button>
            <h3>Record Metadata</h3>
        </div>
        <div class="modal-body"></div>
        <div class="modal-footer"></div>
    </div>


    <div id="cesiumContainer">
        <div id="toolbar"></div>
    </div>

    <div id="searchControls" class="partialaffix span3 search-controls row-fluid nav nav-list well well-small top-pad">
        <table class="nav-table width-full">
            <tr>
                <td><a href="#" class="back nav-link hide"><i class="icon-chevron-left"></i>&nbsp;<span class="backNavText"></span></a></td>
                <td><a href="#" class="forward nav-link pull-right hide"><span class="forwardNavText"></span>&nbsp;<i class="icon-chevron-right"></i></a></td>
            </tr>
        </table>
        <hr class="nav-divider" />
        <div id="searchPages"></div>
    </div>

    <%
    String f = org.codice.ddf.ui.searchui.search.properties.ConfigurationStore
    .getInstance().getFooter();
    if (f != null && f.trim().length() > 0)
    out.println("<div class=\"navbar-fixed-bottom banner\">" + f + "</div>");
    %>

    <!-- Placed at the end of the document so the pages load faster -->

    <script type="text/javascript" src="lib/jquery/js/jquery-1.9.1.min.js"></script>
    <script type="text/javascript"
            src="lib/jquery/js/jquery-ui-1.9.1.custom.min.js"></script>


    <script type="text/javascript"
            src="lib/bootstrap-2.3.1/js/bootstrap.min.js"></script>
    <script type="text/javascript"
            src="lib/bootstrap-extensions/js/partial-affix.js"></script>
    <script type="text/javascript" src="lib/jquery/js/plugin/purl.js"></script>
    <script type="text/javascript"
            src="lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon.js"></script>
    <script type="text/javascript"
            src="lib/jquery/js/plugin/jquery-ui-timepicker-addon.js"></script>

    <!-- These scripts have been compressed and aggregated into Search-min.js.  The list is here for easy modification for
     the sake of debugging.
     TODO: Leverage something like http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/  for a better solution
    -->
    <script type="text/javascript" src="js/searchMessagingDirect.js"></script>
    <script type="text/javascript" src="js/recordView.js"></script>
    <script type="text/javascript" src="js/metadataHelper.js"></script>
    <%--<script type="text/javascript" src="js/viewSwitcher.js"></script>--%>
    <%--<script type="text/javascript" src="js/searchPage.js"></script>--%>


    <script type="text/javascript" src="lib/underscore/underscore.js"></script>
    <script type="text/javascript" src="lib/icanhaz/ICanHaz.min.js"></script>
    <script type="text/javascript" src="lib/backbone/backbone.js"></script>
    <script type="text/javascript" src="lib/backbone-relational/backbone-relational.js"></script>
    <script type="text/javascript" src="lib/modelbinder/Backbone.ModelBinder.min.js"></script>
    <script type="text/javascript" src="lib/modelbinder/Backbone.CollectionBinder.min.js"></script>

    <script type="text/javascript" src="js/model/Metacard.js"></script>
    <script type="text/javascript" src="js/view/MetacardList.view.js"></script>
    <script type="text/javascript" src="js/view/Map.view.js"></script>
    <script type="text/javascript" src="js/view/Query.view.js"></script>
    <script type="text/javascript" src="js/view/SearchControl.view.js"></script>
    <script type="text/javascript" src="js/view/MetacardDetail.view.js"></script>


<!--
 <script type="text/javascript" src="js/Search-min.js"></script>
-->
    <script>
    //TODO figure out another way to do this, this is just for now
    var mapView;

    $(document).ready(function(){
        var promises = [];

        promises.push($.ajax("templates/resultList.html"));
        promises.push($.ajax("templates/searchForm.html"));
        promises.push($.ajax({url: "templates/templates.json", dataType:"json"}));
        $.when.apply(null, promises).done(function(template1, template2, template3, data){
            if (template1 && template1.length > 0 && template2 && template2.length > 0 && template3 && template3.length > 0) {

                ich.addTemplate("resultListTemplate", template1[0]);
                ich.addTemplate("searchFormTemplate", template2[0]);
                _.each(template3[0], function(template) {
                    ich.addTemplate(template.name, template.template);
                });
            }

            init();
        }).fail(function(error){
            alert("Error " + error.statusText);
        });

    });
    init = function() {
        mapView = new MapView();
        mapView.render();

        var searchControlView = new SearchControlView();
        searchControlView.render();
    }
    </script>

</body>
</html>
