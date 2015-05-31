define ['angular', '../Application', 'angular-resource'], (angular, app) ->

  app.factory 'Package', ['$resource', ($resource) ->
    $resource 'projects/:project/packages'
  ]