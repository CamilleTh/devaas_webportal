class AppDetailView extends Backbone.View

  el: "#app-detail"

  templateLoading: _.template( $('#template-app-loading').html() )
  templateGeneral: _.template( $('#template-app-general').html() )
  templateRepository: _.template( $('#template-app-repository').html() )
  templateBuild: _.template( $('#template-app-build').html() )
  templateSubBuild: _.template( $('#template-app-sub-build').html() )
  templateStorage: _.template( $('#template-app-storage').html() )
  templateRuntime: _.template( $('#template-app-runtime').html() )
  templateMonitoring: _.template( $('#template-app-monitoring').html() )
  templateSubMonitoring: _.template( $('#template-app-sub-monitoring').html() )

  events:
    "click .tabGeneralMenu": "showTabGeneral"
    "click .tabRepositoryMenu": "showTabRepository"
    "click .tabStorageMenu": "showTabStorage"
    "click .tabBuildMenu": "showTabBuild"
    "click .tabRuntimeMenu": "showTabRuntime"
    "click .tabMonitoringMenu": "showTabMonitoring"

  initialize: ()->
    @tabGeneral=$ "#tabGeneral"
    @tabStorage=$ "#tabStorage"
    @tabRepository=$ "#tabRepository"
    @tabBuild=$ "#tabBuild"
    @tabRuntime=$ "#tabRuntime"
    @tabMonitoring=$ "#tabMonitoring"
    @currentTab="general"
    @currentEnv="null"

    @render()

  displayDetail: (appId)=>
    @show()
    @applicationId=appId
    @render()

  render: ()=>
    if(@applicationId?)
      @showTabGeneral() if(@currentTab is "general")
      @showTabStorage() if(@currentTab is "storage")
      @showTabRepository() if(@currentTab is "repository")
      @showTabBuild() if(@currentTab is "build")
      @showTabRuntime() if(@currentTab is "runtime")
      @showTabMonitoring() if(@currentTab is "monitoring")
    @

  showTabGeneral: ()=>
    @currentTab="general"
    @tabGeneral.html(@templateLoading())
    $.get("/applications/"+@applicationId, (data)=>
      @tabGeneral.html(@templateGeneral(data))
    )

  showTabStorage: ()=>
    @currentTab="storage"
    @tabStorage.html(@templateLoading())
    $.get("/storages/"+@applicationId, (data)=>
      @tabStorage.html(@templateStorage(data))
    ).error (error)=>
      if error.status is 404 # Storage doesn't exists
        @tabStorage.html(@templateStorage(
          server: null
          storageType: "notfound"
        ))

  showTabRepository: ()=>
    @currentTab="repository"
    @tabRepository.html(@templateLoading())
    $.get("/repositories/"+@applicationId, (data)=>
      @tabRepository.html(@templateRepository(data))
    ).error (error)=>
      if error.status is 404 # Repository doesn't exists
        @tabRepository.html(@templateRepository(
          path: null
        ))

  showTabBuild: ()=>
    @currentTab="build"
    @tabBuild.html(@templateBuild(status:"ok"))
    @subBuildsDiv=$ "#subBuilds"
    @subBuildsDiv.html(@templateLoading())
    $.get("/applications/"+@applicationId, (data)=>
      for fieldname, fieldvalue of data
        @subBuildsDiv.html("")
        if (fieldname == "envs")
          for name, value of fieldvalue
            for envname, val of value
              if(envname == "name")
                $.get("/builds/"+@applicationId+"/"+val, (data)=>
                  lastBuildUrl = data.lastBuildUrl
                  res = lastBuildUrl.split "8080"
                  lastBuildUrl = res[0]+"8080/jenkins"+res[1]
                  data.lastBuildUrl = lastBuildUrl
                  @currentEnv=data.envName
                  @subBuildsDiv.append(@templateSubBuild(data))
                  $("#"+@currentEnv+"_detail").on "click", @detailInfo
                  $("#"+@currentEnv+"_btnRunBuild").on "click", @runBuild
                ).error (error)=>
                  if error.status is 404 # Build doesn't exists
                    @subBuildsDiv.html(@templateSubBuild(
                      jobUrl: null
                    ))
                  if error.status is 500 # Build doesn't exists
                    @subBuildsDiv.html(@templateSubBuild(
                      jobUrl: null
                    ))
    ).error (error)=>
      if error.status is 404 # Build doesn't exists
        @tabBuild.html(@templateBuild(
          status: null
        ))

  showLog: ()=>
    $.get("/logs/"+@applicationId, (data)=>
      $("#logarea").text(data)
    ).error (error)=>
      $("#logarea").text(error.message)

  showTabRuntime: ()=>
    @currentTab="runtime"
    @tabRuntime.html(@templateLoading())
    $.get("/runtimes/"+@applicationId, (data)=>
      @tabRuntime.html(@templateRuntime(data))
      $("#showlogbtn").on "click", @showLog
    ).error (error)=>
      if error.status is 404 # Runtime doesn't exists
        @tabRuntime.html(@templateRuntime(
          server: null
          type: "unknown"
        ))

  showTabMonitoring: ()=>
    @currentTab="monitoring"
    @tabMonitoring.html(@templateMonitoring(status:"ok"))
    @subMonitoringDiv=$ "#subMonitoring"
    @subMonitoringDiv.html(@templateLoading())
    $.get("/applications/"+@applicationId, (data)=>
      for fieldname, fieldvalue of data
        @subMonitoringDiv.html("")
        if (fieldname == "envs")
          for name, value of fieldvalue
            for envname, val of value
              if(envname == "name")
                $.get("/kibana/"+@applicationId+"/"+val, (data)=>
                  data.kibanaUrl = data.kibanaUrl+"/#"+Base64.encode64("{\"search\":\" @fields.application:\\\""+data.appId+"\\\" AND @fields.environment:\\\""+data.envName+"\\\"\",\"fields\":[],\"offset\":0,\"timeframe\":\"all\",\"graphmode\":\"count\",\"time\":{\"user_interval\":0},\"stamp\":0,\"mode\":\"\",\"analyze_field\":\"\"}")
                  @subMonitoringDiv.append(@templateSubMonitoring(data))
                ).error (error)=>
                  console.log(error)
    ).error (error)=>
      if error.status is 404 # Monitoring not found in Chef
        @tabMonitoring.html(@templateMonitoring(
          status: null
        ))


  hide: ()->
    if @$el.css("display") isnt "none"
      @$el.hide("slow")

  show: ()->
    if @$el.css("display") is "none"
      @$el.show("slow")

  detailInfo: (source)=>
    if $("#"+source.target.id+"_icon").hasClass("icon-plus")
      $("#"+source.target.id+"_icon").removeClass("icon-plus")
      $("#"+source.target.id+"_icon").addClass("icon-minus")
      $("#"+source.target.id).addClass("active")
      $("#"+source.target.id).removeClass("btn-warning")
      $("#"+source.target.id+"_info").show("slow")
    else
      $("#"+source.target.id+"_icon").removeClass("icon-minus")
      $("#"+source.target.id+"_icon").addClass("icon-plus")
      $("#"+source.target.id).removeClass("active")
      $("#"+source.target.id).addClass("btn-warning")
      $("#"+source.target.id+"_info").hide("slow")

  runBuild: (source)=>
    console.log(source)
    env = source.target.value
    console.log(env)
    $.ajax(
      type: "GET",
      url: "/builds/run/"+@applicationId+"/"+env,
      contentType: "application/json",
      data: ""
    ).done ()=>
      $("""<div class="alert alert-success"><button type="button" class="close" data-dismiss="alert">x</button>Build of application """+@applicationId+""" on the environment """+env+""" has been triggered...</div>""").insertBefore("div.applicationManager")


window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.AppDetailView=AppDetailView