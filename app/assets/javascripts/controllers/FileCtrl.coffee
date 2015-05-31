define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'FileCtrl', ['$scope', '$location', 'File', 'ProjectService', ($scope, $location, File, ProjectService) ->
    ProjectService.current().then (p) -> $scope.files = File.query project: p.slug
  ]