define ['angular', '../Application'], (angular, app) ->

  app.factory 'ProjectService', ['Project', (Project) ->
    projects = Project.query()
    current = undefined
    current: -> projects.$promise.then (res) ->
      matching = res.filter (p) -> p['slug'] == current
      if matching.length > 0 then matching[0] else undefined
    setCurrent: (slug) -> current = slug
    all: -> projects
    refresh: -> projects = Project.query()
  ]