class AppDetailView extends Backbone.View

  el: "#app-detail"

  templateLoading: _.template( $('#template-app-loading').html() )
  templateGeneral: _.template( $('#template-app-general').html() )
  templateRepository: _.template( $('#template-app-repository').html() )
  templateBuild: _.template( $('#template-app-build').html() )
  templateStorage: _.template( $('#template-app-storage').html() )
  templateRuntime: _.template( $('#template-app-runtime').html() )

  events:
    "click .tabGeneralMenu": "showTabGeneral"
    "click .tabRepositoryMenu": "showTabRepository"
    "click .tabStorageMenu": "showTabStorage"
    "click .tabBuildMenu": "showTabBuild"
    "click .tabRuntimeMenu": "showTabRuntime"

  initialize: ()->
    @tabGeneral=$ "#tabGeneral"
    @tabStorage=$ "#tabStorage"
    @tabRepository=$ "#tabRepository"
    @tabBuild=$ "#tabBuild"
    @tabRuntime=$ "#tabRuntime"
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
    @tabBuild.html(@templateLoading())
    $.get("/applications/"+@applicationId, (data)=>
      for fieldname, fieldvalue of data
        @tabBuild.html("")
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
                  @tabBuild.append(@templateBuild(data))
                  $("btnRunBuild").on "click", @runBuild
                ).error (error)=>
                    if error.status is 404 # Build doesn't exists
                      @tabBuild.html(@templateBuild(
                        jobUrl: null
                      ))
    ).error (error)=>
      console.log("error"+error)

  showLog: ()=>
    console.log("SHOW LOG")
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

  hide: ()->
    if @$el.css("display") isnt "none"
      @$el.hide("slow")

  show: ()->
    if @$el.css("display") is "none"
      @$el.show("slow")

  runBuild: ()=>
    console.log "trigger a build of "+@applicationId
    $.ajax(
      type: "GET",
      url: "/builds/run/"+@applicationId+"/"+@currentEnv,
      contentType: "application/json",
      data: ""
    ).done ()=>
      $("""<div class="alert alert-success"><button type="button" class="close" data-dismiss="alert">x</button>Build of application """+@applicationId+""" has been triggered...</div>""").insertBefore("div.applicationManager")


window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.AppDetailView=AppDetailView