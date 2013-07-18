class AppCreationView extends Backbone.View

  el: "#app-creation"

  templateUser: _.template( $('#template-app-create-users').html() )

  events:
    "click  #cancelBtn": "clear"
    "click  #createBtn": "create"
    "blur #inputAppName": "validateAppName"

  initialize: ()->
    @existingUsers=[]
    @fieldAppName=$("#inputAppName")
    @fieldExistingUsers=$("#existingsUsers")
    @fieldUsername=$("#inputUsername")
    @fieldUserKey=$("#inputUserKey")
    @userList=$("#createUsers")
    @render()
    $("#addUserBtn").on "click",@addUser
    $("#addUserTypeExisting").on "change", ()->
      $(".control-group.new input,textarea").attr("disabled","disabled")
      $(".control-group.existing select").removeAttr("disabled","")
    $("#addUserTypeNew").on "change", ()->
      $(".control-group.new input,textarea").removeAttr("disabled")
      $(".control-group.existing select").attr("disabled","disabled")
    @model=new app.models.App()
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

  clear: ()=>
    @hide()

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
      $.ajax(
        type: "PUT",
        url: "/applications",
        contentType: "application/json",
        data:JSON.stringify(@model.toJSON())
      ).done ()=>
        $("""<div class="alert alert-success"><button type="button" class="close" data-dismiss="alert">x</button>The setup of your application is in progress..</div>""").insertBefore("div.applicationManager")
        @hide()
        window.app.collections.Applications.fetch()

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

  clearAddUser: ()->
    @fieldUsername.val("")
    @fieldUserKey.val("")
    @fieldUsername.parents("div.control-group").removeClass("success")
    @fieldUserKey.parents("div.control-group").removeClass("success")

  renderUserList: ()->
    @userList.html("")
    @addUserToList(user) for user in @model.get("users")

  addUserToList: (user)->
    listItem=$(@templateUser(
      username: user.username
    ))
    listItem.children("a").on("click",()=>
      @removeUser(user.username)
    )
    @userList.append(listItem)

  removeUser: (username)=>
    @model.set("users",@model.get("users").filter (user)->
      user.username isnt username
    )
    @renderUserList()

window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.AppCreationView=AppCreationView