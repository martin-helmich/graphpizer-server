define ['angular', '../Application', 'angular-resource'], (angular, app) ->

  app.factory 'Class', ['$resource', ($resource) ->
    $resource 'projects/:project/model/classes'
  ]