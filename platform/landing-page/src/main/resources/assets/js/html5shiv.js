  


<!DOCTYPE html>
<html>
  <head prefix="og: http://ogp.me/ns# fb: http://ogp.me/ns/fb# githubog: http://ogp.me/ns/fb/githubog#">
    <meta charset='utf-8'>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <title>bootstrap/docs/assets/js/html5shiv.js at master · twitter/bootstrap</title>
    <link rel="search" type="application/opensearchdescription+xml" href="/opensearch.xml" title="GitHub" />
    <link rel="fluid-icon" href="https://github.com/fluidicon.png" title="GitHub" />
    <link rel="apple-touch-icon" sizes="57x57" href="/apple-touch-icon-114.png" />
    <link rel="apple-touch-icon" sizes="114x114" href="/apple-touch-icon-114.png" />
    <link rel="apple-touch-icon" sizes="72x72" href="/apple-touch-icon-144.png" />
    <link rel="apple-touch-icon" sizes="144x144" href="/apple-touch-icon-144.png" />
    <link rel="logo" type="image/svg" href="https://github-media-downloads.s3.amazonaws.com/github-logo.svg" />
    <meta property="og:image" content="https://a248.e.akamai.net/assets.github.com/images/modules/logos_page/Octocat.png">
    <link rel="assets" href="https://a248.e.akamai.net/assets.github.com/">
    <link rel="xhr-socket" href="/_sockets" />
    


    <meta name="msapplication-TileImage" content="/windows-tile.png" />
    <meta name="msapplication-TileColor" content="#ffffff" />
    <meta name="selected-link" value="repo_source" data-pjax-transient />
    <meta content="collector.githubapp.com" name="octolytics-host" /><meta content="github" name="octolytics-app-id" /><meta content="4631300" name="octolytics-actor-id" /><meta content="kcwire" name="octolytics-actor-login" /><meta content="349a52270bcddef23316dae52a0b019eee41340d46a676f640c6757d7ccfa16c" name="octolytics-actor-hash" />

    
    
    <link rel="icon" type="image/x-icon" href="/favicon.ico" />

    <meta content="authenticity_token" name="csrf-param" />
<meta content="5/SvHmXrvEhuHTsLMruzfWleqGCcftpPWTt28QKLPOY=" name="csrf-token" />

    <link href="https://a248.e.akamai.net/assets.github.com/assets/github-77a3b31fd4f61736ec43fef1279c000688f3c8e0.css" media="all" rel="stylesheet" type="text/css" />
    <link href="https://a248.e.akamai.net/assets.github.com/assets/github2-b38c654373748874ee8cf0418033fa848634685f.css" media="all" rel="stylesheet" type="text/css" />
    


      <script src="https://a248.e.akamai.net/assets.github.com/assets/frameworks-46f925b35c476fb905e427274e9dfabed0f19439.js" type="text/javascript"></script>
      <script src="https://a248.e.akamai.net/assets.github.com/assets/github-b615ab69ce3ccc7eb157f21cb8196b6becc2c023.js" type="text/javascript"></script>
      
      <meta http-equiv="x-pjax-version" content="99e964e85bab60c107fc9ad7c499f9c7">

        <link data-pjax-transient rel='permalink' href='/twitter/bootstrap/blob/d991ef2ab1b4d156c7e5d33d052d66f8eaafc460/docs/assets/js/html5shiv.js'>
    <meta property="og:title" content="bootstrap"/>
    <meta property="og:type" content="githubog:gitrepository"/>
    <meta property="og:url" content="https://github.com/twitter/bootstrap"/>
    <meta property="og:image" content="https://secure.gravatar.com/avatar/2f4a8254d032a8ec5e4c48d461e54fcc?s=420&amp;d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-user-420.png"/>
    <meta property="og:site_name" content="GitHub"/>
    <meta property="og:description" content="bootstrap - Sleek, intuitive, and powerful front-end framework for faster and easier web development."/>
    <meta property="twitter:card" content="summary"/>
    <meta property="twitter:site" content="@GitHub">
    <meta property="twitter:title" content="twitter/bootstrap"/>

    <meta name="description" content="bootstrap - Sleek, intuitive, and powerful front-end framework for faster and easier web development." />


    <meta content="50278" name="octolytics-dimension-user_id" /><meta content="twitter" name="octolytics-dimension-user_login" /><meta content="2126244" name="octolytics-dimension-repository_id" /><meta content="twitter/bootstrap" name="octolytics-dimension-repository_nwo" /><meta content="true" name="octolytics-dimension-repository_public" /><meta content="false" name="octolytics-dimension-repository_is_fork" /><meta content="2126244" name="octolytics-dimension-repository_network_root_id" /><meta content="twitter/bootstrap" name="octolytics-dimension-repository_network_root_nwo" />
  <link href="https://github.com/twitter/bootstrap/commits/master.atom" rel="alternate" title="Recent Commits to bootstrap:master" type="application/atom+xml" />

  </head>


  <body class="logged_in page-blob macintosh vis-public env-production  ">

    <div class="wrapper">
      
      
      

      <div class="header header-logged-in true">
  <div class="container clearfix">

    <a class="header-logo-invertocat" href="https://github.com/">
  <span class="mega-octicon octicon-mark-github"></span>
</a>

    <div class="divider-vertical"></div>

      <a href="/notifications" class="notification-indicator tooltipped downwards" title="You have no unread notifications">
    <span class="mail-status all-read"></span>
  </a>
  <div class="divider-vertical"></div>


      <div class="command-bar js-command-bar  in-repository">
          <form accept-charset="UTF-8" action="/search" class="command-bar-form" id="top_search_form" method="get">

<input type="text" data-hotkey="/ s" name="q" id="js-command-bar-field" placeholder="Search or type a command" tabindex="1" autocapitalize="off"
    
    data-username="kcwire"
      data-repo="twitter/bootstrap"
      data-branch="master"
      data-sha="56a2f925b4f018161b76fb5982d79cfeeb39c1e2"
  >

    <input type="hidden" name="nwo" value="twitter/bootstrap" />

    <div class="select-menu js-menu-container js-select-menu search-context-select-menu">
      <span class="minibutton select-menu-button js-menu-target">
        <span class="js-select-button">This repository</span>
      </span>

      <div class="select-menu-modal-holder js-menu-content js-navigation-container">
        <div class="select-menu-modal">

          <div class="select-menu-item js-navigation-item selected">
            <span class="select-menu-item-icon octicon octicon-check"></span>
            <input type="radio" class="js-search-this-repository" name="search_target" value="repository" checked="checked" />
            <div class="select-menu-item-text js-select-button-text">This repository</div>
          </div> <!-- /.select-menu-item -->

          <div class="select-menu-item js-navigation-item">
            <span class="select-menu-item-icon octicon octicon-check"></span>
            <input type="radio" name="search_target" value="global" />
            <div class="select-menu-item-text js-select-button-text">All repositories</div>
          </div> <!-- /.select-menu-item -->

        </div>
      </div>
    </div>

  <span class="octicon help tooltipped downwards" title="Show command bar help">
    <span class="octicon octicon-question"></span>
  </span>


  <input type="hidden" name="ref" value="cmdform">

</form>
        <ul class="top-nav">
            <li class="explore"><a href="/explore">Explore</a></li>
            <li><a href="https://gist.github.com">Gist</a></li>
            <li><a href="/blog">Blog</a></li>
          <li><a href="https://help.github.com">Help</a></li>
        </ul>
      </div>

    

  

    <ul id="user-links">
      <li>
        <a href="/kcwire" class="name">
          <img height="20" src="https://secure.gravatar.com/avatar/4eb229515217f815c5c219decb1262b4?s=140&amp;d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-user-420.png" width="20" /> kcwire
        </a>
      </li>

        <li>
          <a href="/new" id="new_repo" class="tooltipped downwards" title="Create a new repo">
            <span class="octicon octicon-repo-create"></span>
          </a>
        </li>

        <li>
          <a href="/settings/profile" id="account_settings"
            class="tooltipped downwards"
            title="Account settings (You have no verified emails)">
            <span class="octicon octicon-tools"></span>
          </a>
            <span class="settings-warning">!</span>
        </li>
        <li>
          <a class="tooltipped downwards" href="/logout" data-method="post" id="logout" title="Sign out">
            <span class="octicon octicon-log-out"></span>
          </a>
        </li>

    </ul>


<div class="js-new-dropdown-contents hidden">
  

<ul class="dropdown-menu">
  <li>
    <a href="/new"><span class="octicon octicon-repo-create"></span> New repository</a>
  </li>
  <li>
    <a href="/organizations/new"><span class="octicon octicon-list-unordered"></span> New organization</a>
  </li>



    <li class="section-title">
      <span title="twitter/bootstrap">This repository</span>
    </li>
    <li>
      <a href="/twitter/bootstrap/issues/new"><span class="octicon octicon-issue-opened"></span> New issue</a>
    </li>
</ul>

</div>


    
  </div>
</div>

      

      

<div class="flash-global flash-warn">
<div class="container">

    <h2>
      You don't have any verified emails.  We recommend <a href="https://github.com/settings/emails">verifying</a> at least one email.
    </h2>
    <p>
      Email verification helps our support team help you in case you have any email issues or lose your password.
    </p>












</div>
</div>



            <div class="global-notices">
      <div class="flash-global">
        <div class="container">
            <a href="/users/kcwire/enable_repository_next?nwo=twitter%2Fbootstrap" class="button minibutton flash-action blue" data-method="post">Enable Repository Next</a>

            <h2>Hey there, would you like to enable our new repository design?</h2>
            <p>We&rsquo;ve been working hard making a <a href="https://github.com/blog/1529-repository-next">faster, better repository experience</a> and we&rsquo;d love to share it with you.</p>
        </div>
      </div>
    </div>
    <div class="site hfeed" itemscope itemtype="http://schema.org/WebPage">
      <div class="hentry">
        
        <div class="pagehead repohead instapaper_ignore readability-menu ">
          <div class="container">
            <div class="title-actions-bar">
              

<ul class="pagehead-actions">


    <li class="subscription">
      <form accept-charset="UTF-8" action="/notifications/subscribe" data-autosubmit="true" data-remote="true" method="post"><div style="margin:0;padding:0;display:inline"><input name="authenticity_token" type="hidden" value="5/SvHmXrvEhuHTsLMruzfWleqGCcftpPWTt28QKLPOY=" /></div>  <input id="repository_id" name="repository_id" type="hidden" value="2126244" />

    <div class="select-menu js-menu-container js-select-menu">
      <span class="minibutton select-menu-button  js-menu-target">
        <span class="js-select-button">
          <span class="octicon octicon-eye-watch"></span>
          Watch
        </span>
      </span>

      <div class="select-menu-modal-holder">
        <div class="select-menu-modal subscription-menu-modal js-menu-content">
          <div class="select-menu-header">
            <span class="select-menu-title">Notification status</span>
            <span class="octicon octicon-remove-close js-menu-close"></span>
          </div> <!-- /.select-menu-header -->

          <div class="select-menu-list js-navigation-container">

            <div class="select-menu-item js-navigation-item selected">
              <span class="select-menu-item-icon octicon octicon-check"></span>
              <div class="select-menu-item-text">
                <input checked="checked" id="do_included" name="do" type="radio" value="included" />
                <h4>Not watching</h4>
                <span class="description">You only receive notifications for discussions in which you participate or are @mentioned.</span>
                <span class="js-select-button-text hidden-select-button-text">
                  <span class="octicon octicon-eye-watch"></span>
                  Watch
                </span>
              </div>
            </div> <!-- /.select-menu-item -->

            <div class="select-menu-item js-navigation-item ">
              <span class="select-menu-item-icon octicon octicon octicon-check"></span>
              <div class="select-menu-item-text">
                <input id="do_subscribed" name="do" type="radio" value="subscribed" />
                <h4>Watching</h4>
                <span class="description">You receive notifications for all discussions in this repository.</span>
                <span class="js-select-button-text hidden-select-button-text">
                  <span class="octicon octicon-eye-unwatch"></span>
                  Unwatch
                </span>
              </div>
            </div> <!-- /.select-menu-item -->

            <div class="select-menu-item js-navigation-item ">
              <span class="select-menu-item-icon octicon octicon-check"></span>
              <div class="select-menu-item-text">
                <input id="do_ignore" name="do" type="radio" value="ignore" />
                <h4>Ignoring</h4>
                <span class="description">You do not receive any notifications for discussions in this repository.</span>
                <span class="js-select-button-text hidden-select-button-text">
                  <span class="octicon octicon-mute"></span>
                  Stop ignoring
                </span>
              </div>
            </div> <!-- /.select-menu-item -->

          </div> <!-- /.select-menu-list -->

        </div> <!-- /.select-menu-modal -->
      </div> <!-- /.select-menu-modal-holder -->
    </div> <!-- /.select-menu -->

</form>
    </li>

    <li class="js-toggler-container js-social-container starring-container ">
      <a href="/twitter/bootstrap/unstar" class="minibutton with-count js-toggler-target star-button starred upwards" title="Unstar this repo" data-remote="true" data-method="post" rel="nofollow">
        <span class="octicon octicon-star-delete"></span>
        <span class="text">Unstar</span>
      </a>
      <a href="/twitter/bootstrap/star" class="minibutton with-count js-toggler-target star-button unstarred upwards" title="Star this repo" data-remote="true" data-method="post" rel="nofollow">
        <span class="octicon octicon-star"></span>
        <span class="text">Star</span>
      </a>
      <a class="social-count js-social-count" href="/twitter/bootstrap/stargazers">52,139</a>
    </li>

        <li>
          <a href="/twitter/bootstrap/fork" class="minibutton with-count js-toggler-target fork-button lighter upwards" title="Fork this repo" rel="nofollow" data-method="post">
            <span class="octicon octicon-git-branch-create"></span>
            <span class="text">Fork</span>
          </a>
          <a href="/twitter/bootstrap/network" class="social-count">16,825</a>
        </li>


</ul>

              <h1 itemscope itemtype="http://data-vocabulary.org/Breadcrumb" class="entry-title public">
                <span class="repo-label"><span>public</span></span>
                <span class="mega-octicon octicon-repo"></span>
                <span class="author vcard">
                  <a href="/twitter" class="url fn" itemprop="url" rel="author">
                  <span itemprop="title">twitter</span>
                  </a></span> /
                <strong><a href="/twitter/bootstrap" class="js-current-repository">bootstrap</a></strong>
              </h1>
            </div>

            
  <ul class="tabs">
    <li class="pulse-nav"><a href="/twitter/bootstrap/pulse" class="js-selected-navigation-item " data-selected-links="pulse /twitter/bootstrap/pulse" rel="nofollow"><span class="octicon octicon-pulse"></span></a></li>
    <li><a href="/twitter/bootstrap" class="js-selected-navigation-item selected" data-selected-links="repo_source repo_downloads repo_commits repo_tags repo_branches /twitter/bootstrap">Code</a></li>
    <li><a href="/twitter/bootstrap/network" class="js-selected-navigation-item " data-selected-links="repo_network /twitter/bootstrap/network">Network</a></li>
    <li><a href="/twitter/bootstrap/pulls" class="js-selected-navigation-item " data-selected-links="repo_pulls /twitter/bootstrap/pulls">Pull Requests <span class='counter'>64</span></a></li>

      <li><a href="/twitter/bootstrap/issues" class="js-selected-navigation-item " data-selected-links="repo_issues /twitter/bootstrap/issues">Issues <span class='counter'>196</span></a></li>

      <li><a href="/twitter/bootstrap/wiki" class="js-selected-navigation-item " data-selected-links="repo_wiki /twitter/bootstrap/wiki">Wiki</a></li>


    <li><a href="/twitter/bootstrap/graphs" class="js-selected-navigation-item " data-selected-links="repo_graphs repo_contributors /twitter/bootstrap/graphs">Graphs</a></li>


  </ul>
  
<div class="tabnav kill-the-chrome-after-repo-next-ship-tabnav">

  <span class="tabnav-right">
    <ul class="tabnav-tabs">
          <li><a href="/twitter/bootstrap/tags" class="js-selected-navigation-item tabnav-tab" data-selected-links="repo_tags /twitter/bootstrap/tags">Tags <span class="counter ">19</span></a></li>
    </ul>
  </span>

  <div class="tabnav-widget scope">


    <div class="select-menu js-menu-container js-select-menu js-branch-menu">
      <a class="minibutton select-menu-button js-menu-target" data-hotkey="w" data-ref="master">
        <span class="octicon octicon-git-branch"></span>
        <i>branch:</i>
        <span class="js-select-button">master</span>
      </a>

      <div class="select-menu-modal-holder js-menu-content js-navigation-container">

        <div class="select-menu-modal">
          <div class="select-menu-header">
            <span class="select-menu-title">Switch branches/tags</span>
            <span class="octicon octicon-remove-close js-menu-close"></span>
          </div> <!-- /.select-menu-header -->

          <div class="select-menu-filters">
            <div class="select-menu-text-filter">
              <input type="text" id="commitish-filter-field" class="js-filterable-field js-navigation-enable" placeholder="Filter branches/tags">
            </div>
            <div class="select-menu-tabs">
              <ul>
                <li class="select-menu-tab">
                  <a href="#" data-tab-filter="branches" class="js-select-menu-tab">Branches</a>
                </li>
                <li class="select-menu-tab">
                  <a href="#" data-tab-filter="tags" class="js-select-menu-tab">Tags</a>
                </li>
              </ul>
            </div><!-- /.select-menu-tabs -->
          </div><!-- /.select-menu-filters -->

          <div class="select-menu-list select-menu-tab-bucket js-select-menu-tab-bucket css-truncate" data-tab-filter="branches">

            <div data-filterable-for="commitish-filter-field" data-filterable-type="substring">

                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/3.0.0-wip/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="3.0.0-wip" rel="nofollow" title="3.0.0-wip">3.0.0-wip</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/gh-pages/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="gh-pages" rel="nofollow" title="gh-pages">gh-pages</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item selected">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/master/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="master" rel="nofollow" title="master">master</a>
                </div> <!-- /.select-menu-item -->
            </div>

              <div class="select-menu-no-results">Nothing to show</div>
          </div> <!-- /.select-menu-list -->


          <div class="select-menu-list select-menu-tab-bucket js-select-menu-tab-bucket css-truncate" data-tab-filter="tags">
            <div data-filterable-for="commitish-filter-field" data-filterable-type="substring">

                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.3.2/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.3.2" rel="nofollow" title="v2.3.2">v2.3.2</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.3.1/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.3.1" rel="nofollow" title="v2.3.1">v2.3.1</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.3.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.3.0" rel="nofollow" title="v2.3.0">v2.3.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.2.2/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.2.2" rel="nofollow" title="v2.2.2">v2.2.2</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.2.1/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.2.1" rel="nofollow" title="v2.2.1">v2.2.1</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.2.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.2.0" rel="nofollow" title="v2.2.0">v2.2.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.1.1/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.1.1" rel="nofollow" title="v2.1.1">v2.1.1</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.1.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.1.0" rel="nofollow" title="v2.1.0">v2.1.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.0.4/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.0.4" rel="nofollow" title="v2.0.4">v2.0.4</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.0.3/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.0.3" rel="nofollow" title="v2.0.3">v2.0.3</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.0.2/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.0.2" rel="nofollow" title="v2.0.2">v2.0.2</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.0.1/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.0.1" rel="nofollow" title="v2.0.1">v2.0.1</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v2.0.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v2.0.0" rel="nofollow" title="v2.0.0">v2.0.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v1.4.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v1.4.0" rel="nofollow" title="v1.4.0">v1.4.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v1.3.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v1.3.0" rel="nofollow" title="v1.3.0">v1.3.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v1.2.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v1.2.0" rel="nofollow" title="v1.2.0">v1.2.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v1.1.1/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v1.1.1" rel="nofollow" title="v1.1.1">v1.1.1</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v1.1.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v1.1.0" rel="nofollow" title="v1.1.0">v1.1.0</a>
                </div> <!-- /.select-menu-item -->
                <div class="select-menu-item js-navigation-item ">
                  <span class="select-menu-item-icon octicon octicon-check"></span>
                  <a href="/twitter/bootstrap/blob/v1.0.0/docs/assets/js/html5shiv.js" class="js-navigation-open select-menu-item-text js-select-button-text css-truncate-target" data-name="v1.0.0" rel="nofollow" title="v1.0.0">v1.0.0</a>
                </div> <!-- /.select-menu-item -->
            </div>

            <div class="select-menu-no-results">Nothing to show</div>

          </div> <!-- /.select-menu-list -->

        </div> <!-- /.select-menu-modal -->
      </div> <!-- /.select-menu-modal-holder -->
    </div> <!-- /.select-menu -->

  </div> <!-- /.scope -->

  <ul class="tabnav-tabs">
    <li><a href="/twitter/bootstrap" class="selected js-selected-navigation-item tabnav-tab" data-selected-links="repo_source /twitter/bootstrap">Files</a></li>
    <li><a href="/twitter/bootstrap/commits/master" class="js-selected-navigation-item tabnav-tab" data-selected-links="repo_commits /twitter/bootstrap/commits/master">Commits</a></li>
    <li><a href="/twitter/bootstrap/branches" class="js-selected-navigation-item tabnav-tab" data-selected-links="repo_branches /twitter/bootstrap/branches" rel="nofollow">Branches <span class="counter ">3</span></a></li>
  </ul>

</div>

  


            
          </div>
        </div><!-- /.repohead -->

        <div id="js-repo-pjax-container" class="container context-loader-container" data-pjax-container>
          


<!-- blob contrib key: blob_contributors:v21:da5cc21aa6ea592c8edc448492e4de2c -->
<!-- blob contrib frag key: views10/v8/blob_contributors:v21:da5cc21aa6ea592c8edc448492e4de2c -->

<div id="slider">
    <div class="frame-meta">

      <p title="This is a placeholder element" class="js-history-link-replace hidden"></p>

        <a href="/twitter/bootstrap/find/master" class="js-slide-to" data-hotkey="t" style="display:none">Show File Finder</a>

        <div class="breadcrumb">
          <span class='repo-root js-repo-root'><span itemscope="" itemtype="http://data-vocabulary.org/Breadcrumb"><a href="/twitter/bootstrap" class="js-slide-to" data-branch="master" data-direction="back" itemscope="url"><span itemprop="title">bootstrap</span></a></span></span><span class="separator"> / </span><span itemscope="" itemtype="http://data-vocabulary.org/Breadcrumb"><a href="/twitter/bootstrap/tree/master/docs" class="js-slide-to" data-branch="master" data-direction="back" itemscope="url"><span itemprop="title">docs</span></a></span><span class="separator"> / </span><span itemscope="" itemtype="http://data-vocabulary.org/Breadcrumb"><a href="/twitter/bootstrap/tree/master/docs/assets" class="js-slide-to" data-branch="master" data-direction="back" itemscope="url"><span itemprop="title">assets</span></a></span><span class="separator"> / </span><span itemscope="" itemtype="http://data-vocabulary.org/Breadcrumb"><a href="/twitter/bootstrap/tree/master/docs/assets/js" class="js-slide-to" data-branch="master" data-direction="back" itemscope="url"><span itemprop="title">js</span></a></span><span class="separator"> / </span><strong class="final-path">html5shiv.js</strong> <span class="js-zeroclipboard zeroclipboard-button" data-clipboard-text="docs/assets/js/html5shiv.js" data-copied-hint="copied!" title="copy to clipboard"><span class="octicon octicon-clippy"></span></span>
        </div>


        
  <div class="commit file-history-tease">
    <img class="main-avatar" height="24" src="https://secure.gravatar.com/avatar/bc4ab438f7a4ce1c406aadc688427f2c?s=140&amp;d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-user-420.png" width="24" />
    <span class="author"><a href="/mdo" rel="author">mdo</a></span>
    <time class="js-relative-date" datetime="2013-01-14T00:11:55-08:00" title="2013-01-14 00:11:55">January 14, 2013</time>
    <div class="commit-title">
        <a href="/twitter/bootstrap/commit/f9ee99cf6febd0b59ee95aa1866a3c1eb5c61320" class="message">Upgrade to newest HTML5 shiv, and make it a local dependency rather t…</a>
    </div>

    <div class="participation">
      <p class="quickstat"><a href="#blob_contributors_box" rel="facebox"><strong>1</strong> contributor</a></p>
      
    </div>
    <div id="blob_contributors_box" style="display:none">
      <h2>Users on GitHub who have contributed to this file</h2>
      <ul class="facebox-user-list">
        <li>
          <img height="24" src="https://secure.gravatar.com/avatar/bc4ab438f7a4ce1c406aadc688427f2c?s=140&amp;d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-user-420.png" width="24" />
          <a href="/mdo">mdo</a>
        </li>
      </ul>
    </div>
  </div>


    </div><!-- ./.frame-meta -->

    <div class="frames">
      <div class="frame" data-permalink-url="/twitter/bootstrap/blob/d991ef2ab1b4d156c7e5d33d052d66f8eaafc460/docs/assets/js/html5shiv.js" data-title="bootstrap/docs/assets/js/html5shiv.js at master · twitter/bootstrap · GitHub" data-type="blob">

        <div id="files" class="bubble">
          <div class="file">
            <div class="meta">
              <div class="info">
                <span class="icon"><b class="octicon octicon-file-text"></b></span>
                <span class="mode" title="File Mode">file</span>
                  <span>9 lines (8 sloc)</span>
                <span>2.376 kb</span>
              </div>
              <div class="actions">
                <div class="button-group">
                        <a class="minibutton tooltipped leftwards"
                           title="Clicking this button will automatically fork this project so you can edit the file"
                           href="/twitter/bootstrap/edit/master/docs/assets/js/html5shiv.js"
                           data-method="post" rel="nofollow">Edit</a>
                  <a href="/twitter/bootstrap/raw/master/docs/assets/js/html5shiv.js" class="button minibutton " id="raw-url">Raw</a>
                    <a href="/twitter/bootstrap/blame/master/docs/assets/js/html5shiv.js" class="button minibutton ">Blame</a>
                  <a href="/twitter/bootstrap/commits/master/docs/assets/js/html5shiv.js" class="button minibutton " rel="nofollow">History</a>
                </div><!-- /.button-group -->
              </div><!-- /.actions -->

            </div>
                <div class="blob-wrapper data type-javascript js-blob-data">
      <table class="file-code file-diff">
        <tr class="file-code-line">
          <td class="blob-line-nums">
            <span id="L1" rel="#L1">1</span>
<span id="L2" rel="#L2">2</span>
<span id="L3" rel="#L3">3</span>
<span id="L4" rel="#L4">4</span>
<span id="L5" rel="#L5">5</span>
<span id="L6" rel="#L6">6</span>
<span id="L7" rel="#L7">7</span>
<span id="L8" rel="#L8">8</span>

          </td>
          <td class="blob-line-code">
                  <div class="highlight"><pre><div class='line' id='LC1'><span class="cm">/*</span></div><div class='line' id='LC2'><span class="cm"> HTML5 Shiv v3.6.2pre | @afarkas @jdalton @jon_neal @rem | MIT/GPL2 Licensed</span></div><div class='line' id='LC3'><span class="cm">*/</span></div><div class='line' id='LC4'><span class="p">(</span><span class="kd">function</span><span class="p">(</span><span class="nx">l</span><span class="p">,</span><span class="nx">f</span><span class="p">){</span><span class="kd">function</span> <span class="nx">m</span><span class="p">(){</span><span class="kd">var</span> <span class="nx">a</span><span class="o">=</span><span class="nx">e</span><span class="p">.</span><span class="nx">elements</span><span class="p">;</span><span class="k">return</span><span class="s2">&quot;string&quot;</span><span class="o">==</span><span class="k">typeof</span> <span class="nx">a</span><span class="o">?</span><span class="nx">a</span><span class="p">.</span><span class="nx">split</span><span class="p">(</span><span class="s2">&quot; &quot;</span><span class="p">)</span><span class="o">:</span><span class="nx">a</span><span class="p">}</span><span class="kd">function</span> <span class="nx">i</span><span class="p">(</span><span class="nx">a</span><span class="p">){</span><span class="kd">var</span> <span class="nx">b</span><span class="o">=</span><span class="nx">n</span><span class="p">[</span><span class="nx">a</span><span class="p">[</span><span class="nx">o</span><span class="p">]];</span><span class="nx">b</span><span class="o">||</span><span class="p">(</span><span class="nx">b</span><span class="o">=</span><span class="p">{},</span><span class="nx">h</span><span class="o">++</span><span class="p">,</span><span class="nx">a</span><span class="p">[</span><span class="nx">o</span><span class="p">]</span><span class="o">=</span><span class="nx">h</span><span class="p">,</span><span class="nx">n</span><span class="p">[</span><span class="nx">h</span><span class="p">]</span><span class="o">=</span><span class="nx">b</span><span class="p">);</span><span class="k">return</span> <span class="nx">b</span><span class="p">}</span><span class="kd">function</span> <span class="nx">p</span><span class="p">(</span><span class="nx">a</span><span class="p">,</span><span class="nx">b</span><span class="p">,</span><span class="nx">c</span><span class="p">){</span><span class="nx">b</span><span class="o">||</span><span class="p">(</span><span class="nx">b</span><span class="o">=</span><span class="nx">f</span><span class="p">);</span><span class="k">if</span><span class="p">(</span><span class="nx">g</span><span class="p">)</span><span class="k">return</span> <span class="nx">b</span><span class="p">.</span><span class="nx">createElement</span><span class="p">(</span><span class="nx">a</span><span class="p">);</span><span class="nx">c</span><span class="o">||</span><span class="p">(</span><span class="nx">c</span><span class="o">=</span><span class="nx">i</span><span class="p">(</span><span class="nx">b</span><span class="p">));</span><span class="nx">b</span><span class="o">=</span><span class="nx">c</span><span class="p">.</span><span class="nx">cache</span><span class="p">[</span><span class="nx">a</span><span class="p">]</span><span class="o">?</span><span class="nx">c</span><span class="p">.</span><span class="nx">cache</span><span class="p">[</span><span class="nx">a</span><span class="p">].</span><span class="nx">cloneNode</span><span class="p">()</span><span class="o">:</span><span class="nx">r</span><span class="p">.</span><span class="nx">test</span><span class="p">(</span><span class="nx">a</span><span class="p">)</span><span class="o">?</span><span class="p">(</span><span class="nx">c</span><span class="p">.</span><span class="nx">cache</span><span class="p">[</span><span class="nx">a</span><span class="p">]</span><span class="o">=</span><span class="nx">c</span><span class="p">.</span><span class="nx">createElem</span><span class="p">(</span><span class="nx">a</span><span class="p">)).</span><span class="nx">cloneNode</span><span class="p">()</span><span class="o">:</span><span class="nx">c</span><span class="p">.</span><span class="nx">createElem</span><span class="p">(</span><span class="nx">a</span><span class="p">);</span><span class="k">return</span> <span class="nx">b</span><span class="p">.</span><span class="nx">canHaveChildren</span><span class="o">&amp;&amp;!</span><span class="nx">s</span><span class="p">.</span><span class="nx">test</span><span class="p">(</span><span class="nx">a</span><span class="p">)</span><span class="o">?</span><span class="nx">c</span><span class="p">.</span><span class="nx">frag</span><span class="p">.</span><span class="nx">appendChild</span><span class="p">(</span><span class="nx">b</span><span class="p">)</span><span class="o">:</span><span class="nx">b</span><span class="p">}</span><span class="kd">function</span> <span class="nx">t</span><span class="p">(</span><span class="nx">a</span><span class="p">,</span><span class="nx">b</span><span class="p">){</span><span class="k">if</span><span class="p">(</span><span class="o">!</span><span class="nx">b</span><span class="p">.</span><span class="nx">cache</span><span class="p">)</span><span class="nx">b</span><span class="p">.</span><span class="nx">cache</span><span class="o">=</span><span class="p">{},</span><span class="nx">b</span><span class="p">.</span><span class="nx">createElem</span><span class="o">=</span><span class="nx">a</span><span class="p">.</span><span class="nx">createElement</span><span class="p">,</span><span class="nx">b</span><span class="p">.</span><span class="nx">createFrag</span><span class="o">=</span><span class="nx">a</span><span class="p">.</span><span class="nx">createDocumentFragment</span><span class="p">,</span><span class="nx">b</span><span class="p">.</span><span class="nx">frag</span><span class="o">=</span><span class="nx">b</span><span class="p">.</span><span class="nx">createFrag</span><span class="p">();</span></div><div class='line' id='LC5'><span class="nx">a</span><span class="p">.</span><span class="nx">createElement</span><span class="o">=</span><span class="kd">function</span><span class="p">(</span><span class="nx">c</span><span class="p">){</span><span class="k">return</span><span class="o">!</span><span class="nx">e</span><span class="p">.</span><span class="nx">shivMethods</span><span class="o">?</span><span class="nx">b</span><span class="p">.</span><span class="nx">createElem</span><span class="p">(</span><span class="nx">c</span><span class="p">)</span><span class="o">:</span><span class="nx">p</span><span class="p">(</span><span class="nx">c</span><span class="p">,</span><span class="nx">a</span><span class="p">,</span><span class="nx">b</span><span class="p">)};</span><span class="nx">a</span><span class="p">.</span><span class="nx">createDocumentFragment</span><span class="o">=</span><span class="nb">Function</span><span class="p">(</span><span class="s2">&quot;h,f&quot;</span><span class="p">,</span><span class="s2">&quot;return function(){var n=f.cloneNode(),c=n.createElement;h.shivMethods&amp;&amp;(&quot;</span><span class="o">+</span><span class="nx">m</span><span class="p">().</span><span class="nx">join</span><span class="p">().</span><span class="nx">replace</span><span class="p">(</span><span class="sr">/\w+/g</span><span class="p">,</span><span class="kd">function</span><span class="p">(</span><span class="nx">a</span><span class="p">){</span><span class="nx">b</span><span class="p">.</span><span class="nx">createElem</span><span class="p">(</span><span class="nx">a</span><span class="p">);</span><span class="nx">b</span><span class="p">.</span><span class="nx">frag</span><span class="p">.</span><span class="nx">createElement</span><span class="p">(</span><span class="nx">a</span><span class="p">);</span><span class="k">return</span><span class="s1">&#39;c(&quot;&#39;</span><span class="o">+</span><span class="nx">a</span><span class="o">+</span><span class="s1">&#39;&quot;)&#39;</span><span class="p">})</span><span class="o">+</span><span class="s2">&quot;);return n}&quot;</span><span class="p">)(</span><span class="nx">e</span><span class="p">,</span><span class="nx">b</span><span class="p">.</span><span class="nx">frag</span><span class="p">)}</span><span class="kd">function</span> <span class="nx">q</span><span class="p">(</span><span class="nx">a</span><span class="p">){</span><span class="nx">a</span><span class="o">||</span><span class="p">(</span><span class="nx">a</span><span class="o">=</span><span class="nx">f</span><span class="p">);</span><span class="kd">var</span> <span class="nx">b</span><span class="o">=</span><span class="nx">i</span><span class="p">(</span><span class="nx">a</span><span class="p">);</span><span class="k">if</span><span class="p">(</span><span class="nx">e</span><span class="p">.</span><span class="nx">shivCSS</span><span class="o">&amp;&amp;!</span><span class="nx">j</span><span class="o">&amp;&amp;!</span><span class="nx">b</span><span class="p">.</span><span class="nx">hasCSS</span><span class="p">){</span><span class="kd">var</span> <span class="nx">c</span><span class="p">,</span><span class="nx">d</span><span class="o">=</span><span class="nx">a</span><span class="p">;</span><span class="nx">c</span><span class="o">=</span><span class="nx">d</span><span class="p">.</span><span class="nx">createElement</span><span class="p">(</span><span class="s2">&quot;p&quot;</span><span class="p">);</span><span class="nx">d</span><span class="o">=</span><span class="nx">d</span><span class="p">.</span><span class="nx">getElementsByTagName</span><span class="p">(</span><span class="s2">&quot;head&quot;</span><span class="p">)[</span><span class="mi">0</span><span class="p">]</span><span class="o">||</span><span class="nx">d</span><span class="p">.</span><span class="nx">documentElement</span><span class="p">;</span><span class="nx">c</span><span class="p">.</span><span class="nx">innerHTML</span><span class="o">=</span><span class="s2">&quot;x&lt;style&gt;article,aside,figcaption,figure,footer,header,hgroup,nav,section{display:block}mark{background:#FF0;color:#000}&lt;/style&gt;&quot;</span><span class="p">;</span></div><div class='line' id='LC6'><span class="nx">c</span><span class="o">=</span><span class="nx">d</span><span class="p">.</span><span class="nx">insertBefore</span><span class="p">(</span><span class="nx">c</span><span class="p">.</span><span class="nx">lastChild</span><span class="p">,</span><span class="nx">d</span><span class="p">.</span><span class="nx">firstChild</span><span class="p">);</span><span class="nx">b</span><span class="p">.</span><span class="nx">hasCSS</span><span class="o">=!!</span><span class="nx">c</span><span class="p">}</span><span class="nx">g</span><span class="o">||</span><span class="nx">t</span><span class="p">(</span><span class="nx">a</span><span class="p">,</span><span class="nx">b</span><span class="p">);</span><span class="k">return</span> <span class="nx">a</span><span class="p">}</span><span class="kd">var</span> <span class="nx">k</span><span class="o">=</span><span class="nx">l</span><span class="p">.</span><span class="nx">html5</span><span class="o">||</span><span class="p">{},</span><span class="nx">s</span><span class="o">=</span><span class="sr">/^&lt;|^(?:button|map|select|textarea|object|iframe|option|optgroup)$/i</span><span class="p">,</span><span class="nx">r</span><span class="o">=</span><span class="sr">/^(?:a|b|code|div|fieldset|h1|h2|h3|h4|h5|h6|i|label|li|ol|p|q|span|strong|style|table|tbody|td|th|tr|ul)$/i</span><span class="p">,</span><span class="nx">j</span><span class="p">,</span><span class="nx">o</span><span class="o">=</span><span class="s2">&quot;_html5shiv&quot;</span><span class="p">,</span><span class="nx">h</span><span class="o">=</span><span class="mi">0</span><span class="p">,</span><span class="nx">n</span><span class="o">=</span><span class="p">{},</span><span class="nx">g</span><span class="p">;(</span><span class="kd">function</span><span class="p">(){</span><span class="k">try</span><span class="p">{</span><span class="kd">var</span> <span class="nx">a</span><span class="o">=</span><span class="nx">f</span><span class="p">.</span><span class="nx">createElement</span><span class="p">(</span><span class="s2">&quot;a&quot;</span><span class="p">);</span><span class="nx">a</span><span class="p">.</span><span class="nx">innerHTML</span><span class="o">=</span><span class="s2">&quot;&lt;xyz&gt;&lt;/xyz&gt;&quot;</span><span class="p">;</span><span class="nx">j</span><span class="o">=</span><span class="s2">&quot;hidden&quot;</span><span class="k">in</span> <span class="nx">a</span><span class="p">;</span><span class="kd">var</span> <span class="nx">b</span><span class="p">;</span><span class="k">if</span><span class="p">(</span><span class="o">!</span><span class="p">(</span><span class="nx">b</span><span class="o">=</span><span class="mi">1</span><span class="o">==</span><span class="nx">a</span><span class="p">.</span><span class="nx">childNodes</span><span class="p">.</span><span class="nx">length</span><span class="p">)){</span><span class="nx">f</span><span class="p">.</span><span class="nx">createElement</span><span class="p">(</span><span class="s2">&quot;a&quot;</span><span class="p">);</span><span class="kd">var</span> <span class="nx">c</span><span class="o">=</span><span class="nx">f</span><span class="p">.</span><span class="nx">createDocumentFragment</span><span class="p">();</span><span class="nx">b</span><span class="o">=</span><span class="s2">&quot;undefined&quot;</span><span class="o">==</span><span class="k">typeof</span> <span class="nx">c</span><span class="p">.</span><span class="nx">cloneNode</span><span class="o">||</span></div><div class='line' id='LC7'><span class="s2">&quot;undefined&quot;</span><span class="o">==</span><span class="k">typeof</span> <span class="nx">c</span><span class="p">.</span><span class="nx">createDocumentFragment</span><span class="o">||</span><span class="s2">&quot;undefined&quot;</span><span class="o">==</span><span class="k">typeof</span> <span class="nx">c</span><span class="p">.</span><span class="nx">createElement</span><span class="p">}</span><span class="nx">g</span><span class="o">=</span><span class="nx">b</span><span class="p">}</span><span class="k">catch</span><span class="p">(</span><span class="nx">d</span><span class="p">){</span><span class="nx">g</span><span class="o">=</span><span class="nx">j</span><span class="o">=!</span><span class="mi">0</span><span class="p">}})();</span><span class="kd">var</span> <span class="nx">e</span><span class="o">=</span><span class="p">{</span><span class="nx">elements</span><span class="o">:</span><span class="nx">k</span><span class="p">.</span><span class="nx">elements</span><span class="o">||</span><span class="s2">&quot;abbr article aside audio bdi canvas data datalist details figcaption figure footer header hgroup mark meter nav output progress section summary time video&quot;</span><span class="p">,</span><span class="nx">version</span><span class="o">:</span><span class="s2">&quot;3.6.2pre&quot;</span><span class="p">,</span><span class="nx">shivCSS</span><span class="o">:!</span><span class="mi">1</span><span class="o">!==</span><span class="nx">k</span><span class="p">.</span><span class="nx">shivCSS</span><span class="p">,</span><span class="nx">supportsUnknownElements</span><span class="o">:</span><span class="nx">g</span><span class="p">,</span><span class="nx">shivMethods</span><span class="o">:!</span><span class="mi">1</span><span class="o">!==</span><span class="nx">k</span><span class="p">.</span><span class="nx">shivMethods</span><span class="p">,</span><span class="nx">type</span><span class="o">:</span><span class="s2">&quot;default&quot;</span><span class="p">,</span><span class="nx">shivDocument</span><span class="o">:</span><span class="nx">q</span><span class="p">,</span><span class="nx">createElement</span><span class="o">:</span><span class="nx">p</span><span class="p">,</span><span class="nx">createDocumentFragment</span><span class="o">:</span><span class="kd">function</span><span class="p">(</span><span class="nx">a</span><span class="p">,</span><span class="nx">b</span><span class="p">){</span><span class="nx">a</span><span class="o">||</span><span class="p">(</span><span class="nx">a</span><span class="o">=</span><span class="nx">f</span><span class="p">);</span><span class="k">if</span><span class="p">(</span><span class="nx">g</span><span class="p">)</span><span class="k">return</span> <span class="nx">a</span><span class="p">.</span><span class="nx">createDocumentFragment</span><span class="p">();</span></div><div class='line' id='LC8'><span class="k">for</span><span class="p">(</span><span class="kd">var</span> <span class="nx">b</span><span class="o">=</span><span class="nx">b</span><span class="o">||</span><span class="nx">i</span><span class="p">(</span><span class="nx">a</span><span class="p">),</span><span class="nx">c</span><span class="o">=</span><span class="nx">b</span><span class="p">.</span><span class="nx">frag</span><span class="p">.</span><span class="nx">cloneNode</span><span class="p">(),</span><span class="nx">d</span><span class="o">=</span><span class="mi">0</span><span class="p">,</span><span class="nx">e</span><span class="o">=</span><span class="nx">m</span><span class="p">(),</span><span class="nx">h</span><span class="o">=</span><span class="nx">e</span><span class="p">.</span><span class="nx">length</span><span class="p">;</span><span class="nx">d</span><span class="o">&lt;</span><span class="nx">h</span><span class="p">;</span><span class="nx">d</span><span class="o">++</span><span class="p">)</span><span class="nx">c</span><span class="p">.</span><span class="nx">createElement</span><span class="p">(</span><span class="nx">e</span><span class="p">[</span><span class="nx">d</span><span class="p">]);</span><span class="k">return</span> <span class="nx">c</span><span class="p">}};</span><span class="nx">l</span><span class="p">.</span><span class="nx">html5</span><span class="o">=</span><span class="nx">e</span><span class="p">;</span><span class="nx">q</span><span class="p">(</span><span class="nx">f</span><span class="p">)})(</span><span class="k">this</span><span class="p">,</span><span class="nb">document</span><span class="p">);</span></div></pre></div>
          </td>
        </tr>
      </table>
  </div>

          </div>
        </div>

        <a href="#jump-to-line" rel="facebox[.linejump]" data-hotkey="l" class="js-jump-to-line" style="display:none">Jump to Line</a>
        <div id="jump-to-line" style="display:none">
          <form accept-charset="UTF-8" class="js-jump-to-line-form">
            <input class="linejump-input js-jump-to-line-field" type="text" placeholder="Jump to line&hellip;">
            <button type="submit" class="button">Go</button>
          </form>
        </div>

      </div>
    </div>
</div>

<div id="js-frame-loading-template" class="frame frame-loading large-loading-area" style="display:none;">
  <img class="js-frame-loading-spinner" src="https://a248.e.akamai.net/assets.github.com/images/spinners/octocat-spinner-128.gif" height="64" width="64">
</div>


        </div>
      </div>
      <div class="modal-backdrop"></div>
    </div>

    </div><!-- /.wrapper -->

      <div class="container">
  <div class="site-footer">
    <ul class="site-footer-links right">
      <li><a href="https://status.github.com/">Status</a></li>
      <li><a href="http://developer.github.com">Developer</a></li>
      <li><a href="http://training.github.com">Training</a></li>
      <li><a href="http://shop.github.com">Shop</a></li>
      <li><a href="/blog">Blog</a></li>
      <li><a href="/about">About</a></li>
    </ul>

    <a href="/">
      <span class="mega-octicon octicon-mark-github"></span>
    </a>

    <ul class="site-footer-links">
      <li>&copy; 2013 <span title="0.08814s from fe18.rs.github.com">GitHub</span>, Inc.</li>
        <li><a href="/site/terms">Terms</a></li>
        <li><a href="/site/privacy">Privacy</a></li>
        <li><a href="/security">Security</a></li>
        <li><a href="/contact">Contact</a></li>
    </ul>
  </div><!-- /.site-footer -->
</div><!-- /.container -->


    <div class="fullscreen-overlay js-fullscreen-overlay" id="fullscreen_overlay">
  <div class="fullscreen-container js-fullscreen-container">
    <div class="textarea-wrap">
      <textarea name="fullscreen-contents" id="fullscreen-contents" class="js-fullscreen-contents" placeholder="" data-suggester="fullscreen_suggester"></textarea>
          <div class="suggester-container">
              <div class="suggester fullscreen-suggester js-navigation-container" id="fullscreen_suggester"
                 data-url="/twitter/bootstrap/suggestions/commit">
              </div>
          </div>
    </div>
  </div>
  <div class="fullscreen-sidebar">
    <a href="#" class="exit-fullscreen js-exit-fullscreen tooltipped leftwards" title="Exit Zen Mode">
      <span class="mega-octicon octicon-screen-normal"></span>
    </a>
    <a href="#" class="theme-switcher js-theme-switcher tooltipped leftwards"
      title="Switch themes">
      <span class="octicon octicon-color-mode"></span>
    </a>
  </div>
</div>



    <div id="ajax-error-message" class="flash flash-error">
      <span class="octicon octicon-alert"></span>
      <a href="#" class="octicon octicon-remove-close close ajax-error-dismiss"></a>
      Something went wrong with that request. Please try again.
    </div>

    
    <span id='server_response_time' data-time='0.08859' data-host='fe18'></span>
    
  </body>
</html>

