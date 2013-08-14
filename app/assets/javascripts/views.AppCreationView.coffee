class AppCreationView extends Backbone.View

  el: "#app-creation"

  templateUser: _.template( $('#template-app-create-users').html() )
  templateEnv: _.template( $('#template-app-create-envs').html() )
  templateEnvDev: _.template( $('#template-app-create-envs-dev').html() )


  events:
    "click  #createBtn": "create"
    "blur #inputAppName": "validateAppName"

  initialize: ()->
    @existingUsers=[]
    @fieldAppName=$("#inputAppName")
    @fieldGroupId=$("#inputGroupId")
    @fieldExistingUsers=$("#existingsUsers")
    @fieldUsername=$("#inputUsername")
    @fieldEnvironment=$("#inputEnvironment")
    @fieldUserKey=$("#inputUserKey")
    @userList=$("#createUsers")
    @envList=$("#createEnvs")

    @render()
    $("#addUserBtn").on "click",@addUser
    $("#addUserTypeExisting").on "change", ()->
      $(".control-group.new input,textarea").attr("disabled","disabled")
      $(".control-group.existing select").removeAttr("disabled","")
    $("#addUserTypeNew").on "change", ()->
      $(".control-group.new input,textarea").removeAttr("disabled")
      $(".control-group.existing select").attr("disabled","disabled")
    $("#addEnvBtn").on "click",@addEnv
    @model=new app.models.App()
    @renderEnvList()
    $.get "/users", (result)=>
      @fieldExistingUsers.html("")
      @fieldExistingUsers.append("<option value=\""+user.username+"\">"+user.username+"</option>") for user in result
      @existingUsers[user.username]=user.userkey for user in result

  render: ()=>
    @

  hide: ()->
    if @$el.css("display") isnt "none"
      @$el.hide("slow")

  show: ()->
    if @$el.css("display") is "none"
      @$el.show("slow")

  validateAppName: (event,andThen)->
    $.get "/validate/appId?appId="+@fieldAppName.val(), (result)=>
      if result.valid isnt true
        @fieldAppName.parents("div.control-group").removeClass("success")
        @fieldAppName.parents("div.control-group").addClass("error")
        @fieldAppName.siblings(".help-inline").html(result.error)
        @nameValid=false
      else
        @fieldAppName.parents("div.control-group").removeClass("error")
        @fieldAppName.parents("div.control-group").addClass("success")
        @fieldAppName.siblings("span.help-inline").html("Name is valid")
        @nameValid=true
      andThen() if andThen?

  create: ()=>
    @validateAppName null,@doCreation

  doCreation: ()=>
    if @nameValid? and @nameValid is true
      @model.set("type",$("#inputAppType").val())
      @model.set("id",@fieldAppName.val())
      @model.set("storageType",$("#inputAppStorage").val())
      @model.set("groupid",@fieldGroupId.val())
      @model.set("port","8080")
      $.ajax(
        type: "PUT",
        url: "/applications",
        contentType: "application/json",
        data:JSON.stringify(@model.toJSON())
      ).done ()=>
        for env in @model.get("envs")
          $("""<div class="alert alert-warning" style="width:55%"><button type="button" class="close" data-dismiss="alert">x</button><div class="infoData"></div><div class="progress progress-warning progress-striped"><div class="bar" style="width: 0%"></div></div></div>""").attr('id', @model.id+"_"+env.name+"_001_progression").insertBefore("div.applicationManager")
        @interval = setInterval(@updateProgressBars, 5000)
        @hide()
        window.app.collections.Applications.fetch()

  updateProgressBars: ()=>
    for env in @model.get("envs")
      vmname = @model.id+"_"+env.name+"_001"
      @progressBar(vmname)

  progressBar: (vmname)=>
    $.get "/progression/"+vmname, (result)=>
      if result.isDeployed=="true"
        $("#"+vmname+"_progression").children(".progress").children(".bar").attr('style',"width:100%")
        $("#"+vmname+"_progression").children(".infoData").html(vmname+" : Success!")
      if result.isDeployed=="false"
        totalsteps=13
        stepnumber=result.stepnumber
        stepname=result.stepname
        progression=(stepnumber/totalsteps)*100
        $("#"+vmname+"_progression").children(".progress").children(".bar").attr('style',"width:"+progression+"%")
        $("#"+vmname+"_progression").children(".infoData").html(vmname+" : Step "+stepnumber+" / "+totalsteps)
    .error (error)=>
      $("#"+vmname+"_progression").children(".infoData").html("Not Found")


  addUser: ()=>
    if $("input[name='addUserType']:checked").val() is "new"
      $.get "/validate/username?username="+@fieldUsername.val(), (result)=>
        if result.valid isnt true
          @fieldUsername.parents("div.control-group").removeClass("success")
          @fieldUsername.parents("div.control-group").addClass("error")
          $("#helpUsername").html(result.error)
        else
          @fieldUsername.parents("div.control-group").removeClass("error")
          @fieldUsername.parents("div.control-group").addClass("success")
          $("#helpUsername").html("")
          $.ajax(
            type: "PUT",
            url: "/validate/userKey",
            contentType: "text/plain"
            data: @fieldUserKey.val()
          ).done (data)=>
            if data.valid isnt true
              @fieldUserKey.parents("div.control-group").removeClass("success")
              @fieldUserKey.parents("div.control-group").addClass("error")
              $("#helpUserKey").html(data.error)
            else
              @model.get("users").push
                username: @fieldUsername.val()
                userkey: @fieldUserKey.val()
              @renderUserList()
              @fieldUserKey.parents("div.control-group").removeClass("error")
              @fieldUserKey.parents("div.control-group").addClass("success")
              $("#helpUserKey").html("")
              @clearAddUser()
              $("#addUserModal").modal("hide")
    else
      if @fieldExistingUsers.val() isnt null
        @model.get("users").push
          username: @fieldExistingUsers.val()
          userkey: @existingUsers[@fieldExistingUsers.val()]
        @renderUserList()
        @fieldUserKey.parents("div.control-group").removeClass("error")
        @fieldUserKey.parents("div.control-group").addClass("success")
        @clearAddUser()
        $("#addUserModal").modal("hide")

  addEnv: ()=>
    $("#helpEnvironment").html("")
    exists: false
    for env in @model.get("envs")
      if @fieldEnvironment.val() == env.name
        exists=true
        $("#helpEnvironment").html("Env name already in use")
        @fieldEnvironment.parents("div.control-group").addClass("error")
    if !exists
      @model.get("envs").push
        name : @fieldEnvironment.val()
        version : 'null'
      @fieldEnvironment.parents("div.control-group").removeClass("error")
      @renderEnvList()
      $("#addEnvModal").modal("hide")

  clearAddUser: ()->
    @fieldUsername.val("")
    @fieldUserKey.val("")
    @fieldUsername.parents("div.control-group").removeClass("success")
    @fieldUserKey.parents("div.control-group").removeClass("success")

  renderUserList: ()->
    @userList.html("")
    @addUserToList(user) for user in @model.get("users")

  renderEnvList: ()->
    @envList.html("")
    @addEnvToList(env) for env in @model.get("envs")

  addUserToList: (user)->
    listItem=$(@templateUser(
      username: user.username
    ))
    listItem.children("a").on("click",()=>
      @removeUser(user.username)
    )
    @userList.append(listItem)

  addEnvToList: (env)->
    if env.name != "Dev"
      listItem=$(@templateEnv(
        env: env.name
      ))
      listItem.children("a").on("click",()=>
        @removeEnv(env.name)
      )
    else
      listItem=$(@templateEnvDev(
        env: env.name
      ))
    @envList.append(listItem)

  removeUser: (username)=>
    @model.set("users",@model.get("users").filter (user)->
      user.username isnt username
    )
    @renderUserList()

  removeEnv: (env)=>
    @model.set("envs",@model.get("envs").filter (envi)->
      envi.name isnt env
    )
    @renderEnvList()

window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.AppCreationView=AppCreationView