define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ClassCtrl', ['$scope', '$location', 'Class', 'ProjectService', ($scope, $location, Class, ProjectService) ->
    ProjectService.current().then (p) -> $scope.files = Class.query project: p.slug
  ]