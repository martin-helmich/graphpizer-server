define ['angular', '../Application', 'angular-resource'], (angular, app) ->

  app.factory 'File', ['$resource', ($resource) ->
    $resource 'projects/:project/files'
  ]