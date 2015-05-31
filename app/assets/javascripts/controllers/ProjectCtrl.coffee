define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectCtrl', ['$scope', '$rootScope', '$routeParams', '$q', 'ProjectService', ($scope, $rootScope, $routeParams, $q, ProjectService) ->
    $scope.projects = ProjectService.all()

    $rootScope.$on '$routeChangeStart', (e, next) ->
      if next.params['project']?
        slug = next.params['project']
        ProjectService.setCurrent slug
        ProjectService.current().then (p) -> $scope.currentProject = p

    refresh = -> $scope.projects = ProjectService.refresh()

    $scope.$on 'projectCreated', refresh
    $scope.$on 'projectDeleted', refresh
  ]