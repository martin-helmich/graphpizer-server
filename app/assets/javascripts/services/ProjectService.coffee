# GraPHPizer source code analytics engine
# Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

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