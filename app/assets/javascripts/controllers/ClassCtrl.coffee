define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ClassCtrl', ['$scope', '$location', 'Class', 'ProjectService', ($scope, $location, Class, ProjectService) ->
    ProjectService.current().then (p) -> $scope.classes = Class.query project: p.slug
  ]