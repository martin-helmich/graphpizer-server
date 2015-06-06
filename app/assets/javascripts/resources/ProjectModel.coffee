define ['angular', '../Application', 'angular-resource'], (angular, app) ->

  app.factory 'ProjectModel', ['$resource', ($resource) -> $resource 'projects/:id/model']