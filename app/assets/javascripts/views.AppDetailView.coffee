class AppDetailView extends Backbone.View

  el: "#app-detail"

  templateLoading: _.template( $('#template-app-loading').html() )
  templateGeneral: _.template( $('#template-app-general').html() )
  templateRepository: _.template( $('#template-app-repository').html() )
  templateBuild: _.template( $('#template-app-build').html() )
  templateStorage: _.template( $('#template-app-storage').html() )
  templateRuntime: _.template( $('#template-app-runtime').html() )
  templateMonitoring: _.template( $('#template-app-monitoring').html() )

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
    @envName="null"
    @render()

  displayDetail: (appId,env)=>
    @show()
    @applicationId=appId
    @envName=env
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

  currentButtonSwitch: (nextButton)=>
    $("#"+@currentTab).attr("disabled","")
    $("#"+nextButton).attr("disabled","disabled")
    @currentTab=nextButton

  showTabGeneral: ()=>
    @currentButtonSwitch("general")
    @tabGeneral.html(@templateLoading())
    $.get("/applications/"+@applicationId, (data)=>
      for fieldname, fieldvalue of data
        if(fieldname == "url")
          if(fieldvalue != "none")
            for envName, url of fieldvalue
              if(envName == @envName)
                data.envUrl=url
                data.envName=envName
                @tabGeneral.html(@templateGeneral(data))
    ).error (error)=>
      if error.status is 404 # Not found
        @tabGeneral.html(@templateGeneral(
          status: null
        ))

  showTabRepository: ()=>
    @currentButtonSwitch("repository")
    @tabRepository.html(@templateLoading())
    $.get("/repositories/"+@applicationId, (data)=>
      @tabRepository.html(@templateRepository(data))
    ).error (error)=>
      if error.status is 404 # Repository doesn't exists
        @tabRepository.html(@templateRepository(
          path: null
        ))

  showTabBuild: ()=>
    @currentButtonSwitch("build")
    @tabBuild.html(@templateLoading())
    $.get("/builds/"+@applicationId+"/"+@envName, (data)=>
      @tabBuild.html("")
      lastBuildUrl = data.lastBuildUrl
      res = lastBuildUrl.split "8080"
      lastBuildUrl = res[0]+"8080/jenkins"+res[1]
      data.lastBuildUrl = lastBuildUrl
      data.envName = @envName
      @tabBuild.html(@templateBuild(data))
      $("#"+@envName+"_detail").on "click", @buildJobInfo
      $("#"+@envName+"_btnRunBuild").on "click", @runBuild
    ).error (error)=>
      if error.status is 404 # Build doesn't exists
        @tabBuild.html(@templateBuild(
          jobUrl: null
        ))
      if error.status is 500 # Build doesn't exists
        @tabBuild.html(@templateBuild(
          jobUrl: null
        ))

  showTabStorage: ()=>
    @currentButtonSwitch("storage")
    @tabStorage.html(@templateLoading())
    $.get("/storages/"+@applicationId, (data)=>
      @tabStorage.html("")
      for fieldname, fieldvalue of data
        if (fieldname == "envs")
          for env, envAttrs of fieldvalue
            if (env == @envName)
              @tabStorage.append(@templateStorage(
                env : env
                storageType : data.storageType
                dbName : envAttrs.dbName
                user : envAttrs.user
                server : envAttrs.url
                password : envAttrs.password
                port : envAttrs.port
              ))
    ).error (error)=>
      if error.status is 404 # Storage doesn't exists
        @tabStorage.html(@templateStorage(
          server: null
          storageType: "notfound"
        ))

  showLog: ()=>
    $.get("/logs/"+@applicationId, (data)=>
      $("#logarea").text(data)
    ).error (error)=>
      $("#logarea").text(error.message)

  showTabRuntime: ()=>
    @currentButtonSwitch("runtime")
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
    @currentButtonSwitch("monitoring")
    @tabMonitoring.html(@templateLoading())
    $.get("/kibana/"+@applicationId+"/"+@envName, (data)=>
      data.kibanaUrl = data.kibanaUrl+"/#"+Base64.encode64("{\"search\":\" @fields.application:\\\""+data.appId+"\\\" AND @fields.environment:\\\""+data.envName+"\\\"\",\"fields\":[],\"offset\":0,\"timeframe\":\"all\",\"graphmode\":\"count\",\"time\":{\"user_interval\":0},\"stamp\":0,\"mode\":\"\",\"analyze_field\":\",\"}")
      @tabMonitoring.html(@templateMonitoring(data))
    ).error (error)=>
      if error.status is 404 # Monitoring not found
        @tabMonitoring.html(@templateMonitoring(
          status: null
        ))

  hide: ()->
    if @$el.css("display") isnt "none"
      @$el.hide("slow")

  show: ()->
    if @$el.css("display") is "none"
      @$el.show("slow")

  buildJobInfo: (source)=>
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