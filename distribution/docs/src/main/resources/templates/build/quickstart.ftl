<#list quickStarts?sort_by("order") as quickStartIntro>
<#if (quickStartIntro.status == "published"
    && quickStartIntro.section?contains("quickStart")
    && quickStartIntro.level?contains("intro"))>

include::${quickStartIntro.file}[]
<#list quickStarts?sort_by("order") as quickStart>

<#if (quickStart.status == "published"
    && quickStart.section?contains("quickStart")
    && quickStart.level?contains("section")
    && quickStart.parent == quickStartIntro.title)>

include::${quickStart.file}[leveloffset=+1]

<#list quickStarts?sort_by("order") as subQuickStart>
<#if (subQuickStart.status == "published"
    && subQuickStart.section?contains("quickStart")
    && quickStart.level?contains("subSection")
    && subQuickStart.parent == subQuickStartIntro.title)>

include::${quickStart.file}[leveloffset=+2]
level 2
</#if>
</#list>
</#if>
</#list>
</#if>
</#list>

