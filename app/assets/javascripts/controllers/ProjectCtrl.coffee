define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectCtrl', ['$scope', '$rootScope', '$routeParams', 'Project', ($scope, $rootScope, $routeParams, Project) ->
    $scope.projects = Project.query()
    $scope.currentProject = $routeParams['project']

    $rootScope.$on '$routeChangeStart', (e, next) ->
      $scope.currentProject = if next.params['project']? then next.params['project'] else undefined

    refresh = -> $scope.projects = Project.query()

    $scope.$on 'projectCreated', refresh
    $scope.$on 'projectDeleted', refresh
  ]