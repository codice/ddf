$(function () {
    // Search Items
    $('#search').on('keyup', function (e) {
        var value = $(this).val();
        var $el = $('.navigation');

        if (value) {
            var regexp = new RegExp(value, 'i');
            $el.find('li, .itemMembers').hide();

            $el.find('li').each(function (i, v) {
                var $item = $(v);

                if ($item.data('name') && regexp.test($item.data('name'))) {
                    $item.show();
                    $item.closest('.itemMembers').show();
                    $item.closest('.item').show();
                }
            });
        } else {
            $el.find('.item, .itemMembers').show();
        }

        $el.find('.list').scrollTop(0);
    });

    // Toggle when click an item element
    $('.navigation').on('click', '.title', function (e) {
        $(this).parent().find('.itemMembers').toggle();
    });

    // Show an item related a current documentation automatically
    var filename = $('.page-title').data('filename').replace(/\.[a-z]+$/, '');
    var $currentItem = $('.navigation .item[data-name*="' + filename + '"]:eq(0)');

    if ($currentItem.length) {
        $currentItem
            .remove()
            .prependTo('.navigation .list')
            .show()
            .find('.itemMembers')
                .show();
    }

    // Auto resizing on navigation
    var _onResize = function () {
        var height = $(window).height();
        var $el = $('.navigation');

        $el.height(height).find('.list').height(height - 133);
    };

    $(window).on('resize', _onResize);
    _onResize();

    var currentVersion = document.getElementById('package-version').innerHTML;

    // warn about outdated version
    var packageUrl = 'https://raw.githubusercontent.com/openlayers/openlayers.github.io/build/package.json';
    fetch(packageUrl).then(function(response) {
      return response.json();
    }).then(function(json) {
      var latestVersion = json.version;
      document.getElementById('latest-version').innerHTML = latestVersion;
      var url = window.location.href;
      var branchSearch = url.match(/\/([^\/]*)\/apidoc\//);
      var cookieText = 'dismissed=-' + latestVersion + '-';
      var dismissed = document.cookie.indexOf(cookieText) != -1;
      if (!dismissed && /^v[0-9\.]*$/.test(branchSearch[1]) && currentVersion != latestVersion) {
        var link = url.replace(branchSearch[0], '/latest/apidoc/');
        fetch(link, {method: 'head'}).then(function(response) {
          var a = document.getElementById('latest-link');
          a.href = response.status == 200 ? link : '../../latest/apidoc/';
        });
        var latestCheck = document.getElementById('latest-check');
        latestCheck.style.display = '';
        document.getElementById('latest-dismiss').onclick = function() {
          latestCheck.style.display = 'none';
          document.cookie = cookieText;
        }
      }
    });

    // create source code links to github
    var srcLinks = $('div.tag-source');
    srcLinks.each(function(i, el) {
      var textParts = el.innerHTML.trim().split(', ');
      var link = 'https://github.com/openlayers/ol3/blob/v' + currentVersion + '/' +
          textParts[0];
      el.innerHTML = '<a href="' + link + '">' + textParts[0] + '</a>, ' +
          '<a href="' + link + textParts[1].replace('line ', '#l') + '">' +
          textParts[1] + '</a>';
    });

    // show/hide unstable items
    var links = $('a[href^="ol."]');
    var unstable = $('.unstable');
    var stabilityToggle = $('#stability-toggle');
    stabilityToggle.change(function() {
        unstable.toggleClass('hidden', this.checked);
        var search = this.checked ? '?stableonly=true' : '';
        links.each(function(i, el) {
            this.href = this.pathname + search + this.hash;
        });
        if (history.replaceState) {
            var url = window.location.pathname + search + window.location.hash;
            history.replaceState({}, '', url);
        }
        return false;
    });
    var search = window.location.search;
    links.each(function(i, el) {
        this.href = this.pathname + search + this.hash;
    });
    stabilityToggle.prop('checked', search === '?stableonly=true');
    unstable.toggleClass('hidden', stabilityToggle[0].checked);
});
