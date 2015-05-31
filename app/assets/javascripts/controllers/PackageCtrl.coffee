define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'PackageCtrl', ['$scope', '$location', 'Package', 'ProjectService', ($scope, $location, Package, ProjectService) ->
    ProjectService.current().then (p) -> $scope.packages = Package.query project: p.slug
  ]