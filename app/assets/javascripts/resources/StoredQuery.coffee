define ['angular', '../Application', 'angular-resource'], (angular, app) ->

  app.factory 'StoredQuery', ['$resource', ($resource) ->
    $resource 'queries/:id', 'id': '@id',
      save:
        method: 'POST'
      update:
        method: 'PUT'
  ]