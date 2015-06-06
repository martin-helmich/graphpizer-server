define ['angular', '../Application', 'angular-resource'], (angular, app) ->

  app.factory 'Project', ['$resource', ($resource) ->
    $resource 'projects/:id', 'id': '@slug',
      save:
        method: 'PUT'
      generateModel:
        method: 'POST'
        url: 'projects/:id/model/generate'
  ]