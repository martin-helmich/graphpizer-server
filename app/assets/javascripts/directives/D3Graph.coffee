define ['angular', '../Application'], (angular, app) ->
  app.directive 'd3Graph', ['$window', ($window) ->
    restrict: 'EA'
    scope:
      nodes: '=nodes'
      edges: '=edges'
    link: (scope, element, attrs) ->
      width = 960
      height = 500

      window.onresize = -> scope.$apply()

      scope.$watch(
        -> (angular.element $window)[0].innerWidth,
        -> scope.render scope.nodes, scope.edges
      )

      nodeIdMap = {}
      scope.nodes.forEach (node, idx) -> nodeIdMap[node.id] = idx
      scope.edges.forEach (edge) ->
        edge.source = nodeIdMap[edge.start]
        edge.target = nodeIdMap[edge.end]

      nodeColor = d3.scale.category20()

      scope.render = (nodes, edges) ->
        force = d3.layout.force()
        .charge -180
#        .chargeDistance 40
        .linkDistance 50
        .size [width, height]

        svg = d3.select element[0]
        .append 'svg'
        .style 'width', '100%'
        .style 'height', '600'

        container = svg.append 'g'
        container.append("svg:defs").selectAll("marker")
        .data(["unidir", "implements"])
        .enter()
        .append("svg:marker")
        .attr("id", String)
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 16)
        .attr("refY", 0)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto")
        .append("svg:path")
        .attr("d", "M0,-5L10,0L0,5L0,-5")
        .attr("class", "marker")
        .attr("fill", '#999')
        .style("stroke-width", 1)

        force
        .nodes nodes
        .links edges
        .start()

        link = container
        .selectAll ".link"
        .data edges
        .enter()
        .append "line"
        .attr "class", "link"
        .attr 'marker-end', 'unidir'
        .style "stroke-width", 1

        node = container.selectAll(".node")
        .data(nodes)
        .enter()
        .append("circle")
        .attr("class", "node")
        .attr("r", (d) -> 20)
        .style("fill", (d) -> nodeColor(d.labels[0]))
        .call(force.drag);

        node.on 'mouseover', (d) ->
          link.classed "link-active", (l) -> (l.source == d) || (l.target == d)
          d3.select(this).classed "node-active", true
        node.on 'mouseout', (d) ->
          node.classed "node-active", false
          link.classed "link-active", false

        force.on("tick", ->
          link
          .attr("x1", (d) -> d.source.x)
          .attr("y1", (d) -> d.source.y)
          .attr("x2", (d) -> d.target.x)
          .attr("y2", (d) -> d.target.y);

          node
          .attr("cx", (d) -> d.x)
          .attr("cy", (d) -> d.y)
        )
  ]

#    (input, cls = 'mute') ->
#      if typeof(input) is "string"
#        namespaces = input.split '\\'
#        "<span class=\"#{cls}\">" + (namespaces[0..-1].join "\\") + "\\</span>" + namespaces[-1]