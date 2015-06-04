define ['angular', '../Application'], (angular, app) ->

  app.directive 'cypherRelationship', () ->
    restrict: 'E'
    scope:
      relationship: '=relationship'
    templateUrl: 'assets/partials/cypher/relationship.html'

#    (input, cls = 'mute') ->
#      if typeof(input) is "string"
#        namespaces = input.split '\\'
#        "<span class=\"#{cls}\">" + (namespaces[0..-1].join "\\") + "\\</span>" + namespaces[-1]