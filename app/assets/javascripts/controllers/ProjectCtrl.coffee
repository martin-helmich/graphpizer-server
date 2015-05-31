define ['angular', '../Application', '../resources/Project'], (angular, app) ->

  app.controller 'ProjectCtrl', ['$rootScope', '$routeParams', 'Project', ($rootScope, $routeParams, Project) ->
    $rootScope.projects = Project.query()
    $rootScope.currentProject = $routeParams['project']

    $rootScope.$on '$routeChangeStart', (e, next) ->
      $rootScope.currentProject = if next.params['project']? then next.params['project'] else undefined
  ]