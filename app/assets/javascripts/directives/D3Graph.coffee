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

      nodeWidth = 25

      nodeIdMap = {}
      scope.nodes.forEach (node, idx) -> nodeIdMap[node.id] = idx
      scope.edges.forEach (edge) ->
        edge.source = nodeIdMap[edge.start]
        edge.target = nodeIdMap[edge.end]

      zoom = d3.behavior.zoom()
      .scaleExtent [1, 10]
      .on "zoom", -> container.attr "transform", "translate(#{d3.event.translate})scale(#{d3.event.scale})"

      svg = d3.select element[0]
        .append 'svg'
        .style 'width', '100%'
        .style 'height', '600'
      .append "g"
#      .attr "transform", "translate(-5,-5)"
#      .call zoom

#      rect = svg.append "rect"
#      .attr "width", "100%"
#      .attr "height", 600
#      .style "fill", "none"
#      .style "pointer-events", "all"

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

      nodeColor = d3.scale.category20()

      force = d3.layout.force()
      .charge -270
      .linkDistance (nodeWidth * 5)
      .size [width, height]

#      drag = d3.behavior.drag()
#      .origin (d) -> d
#      .on "dragstart", ->
#        d3.event.sourceEvent.stopPropagation()
#        d3.select(this).classed "dragging", true
#        force.start()
#      .on "drag", (d) ->
#        d3.select(this)
#        .attr "cx", d.x = d3.event.x
#        .attr "cy", d.y = d3.event.y
#      .on "dragend", -> d3.select(this).classed "dragging", false

      scope.render = (nodes, edges) ->

        force
        .nodes nodes
        .links edges
        .start()

        link = container
        .selectAll ".link"
        .data edges
        .enter()
        .append "svg:path"
        .attr "id", (d) -> "edge-#{d.id}"
        .attr "class", "link"
        .attr 'marker-end', 'unidir'
        .style "stroke-width", 1

        linktext = svg.append "svg:g"
        .selectAll "g.linklabelholder"
        .data edges

        linktext.enter()
        .append "g"
        .attr "class", "linklabelholder"
        .append "text"
        .style "text-anchor", "middle"
        .style "font-size", "8px"
        .attr "dy", -2
        .append "textPath"
        .attr "xlink:href", (d) -> "#edge-#{d.id}"
        .attr "startOffset", "50%"
        .text (d) -> d.label

        node = container.selectAll(".node")
        .data nodes
        .enter()
        .append "g"
        .attr "class", "node"
        .call force.drag

        node.append "circle"
        .attr "r", (d) -> nodeWidth
        .attr "x", -nodeWidth / 2
        .attr "y", -nodeWidth / 2
        .style("fill", (d) -> nodeColor(d.labels[0]))
#        .call(drag);

        node.on 'mouseover', (d) ->
          link.classed "link-active", (l) -> (l.source == d) || (l.target == d)
          d3.select(this).classed "node-active", true
        node.on 'mouseout', (d) ->
          node.classed "node-active", false
          link.classed "link-active", false

        node.append "text"
        .attr "class", "node-id"
        .attr "dx", nodeWidth + 5
        .attr "dy", ".35em"
        .text (d) -> d.id

        force.on("tick", ->
#          link
#          .attr("x1", (d) -> d.source.x)
#          .attr("y1", (d) -> d.source.y)
#          .attr("x2", (d) -> d.target.x)
#          .attr("y2", (d) -> d.target.y);
          link.attr "d", (d) -> "M#{d.source.x},#{d.source.y} #{d.target.x},#{d.target.y}"

          node.attr "transform", (d) -> "translate(#{d.x}, #{d.y})"
          link.attr "transform", (d) -> "translate(#{d.x}, #{d.y})"
        )
  ]

#    (input, cls = 'mute') ->
#      if typeof(input) is "string"
#        namespaces = input.split '\\'
#        "<span class=\"#{cls}\">" + (namespaces[0..-1].join "\\") + "\\</span>" + namespaces[-1]