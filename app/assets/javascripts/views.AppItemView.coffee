class AppItemView extends Backbone.View

  tagName:  'li'

  template: _.template( $('#template-app-item').html() )

  render: ()=>
    @$el.html( @template( @model.toJSON() ) )
    @$el.addClass(@model.get("id"))
    if @options.selected is true
      @$el.addClass("active")
    @

window.app=window.app || {}
window.app.views=window.app.views || {}
window.app.views.AppItemView=AppItemView