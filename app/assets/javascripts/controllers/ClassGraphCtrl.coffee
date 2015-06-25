define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ClassGraphCtrl', ['$scope', '$http', 'ProjectService', ($scope, $http, ProjectService) ->
    ProjectService.current().then (p) ->
  ]