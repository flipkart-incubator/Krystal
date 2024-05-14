package com.flipkart.krystal.krystex.logicdecorators.observability;

/**
 * @author ajit.dwivedi on 20/02/24
 */
class GenerateHtml {
  public static String generateHtml(String jsonData) {
    return """
      <!DOCTYPE html>
      <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Collapsible Tree</title>
          <script src="https://d3js.org/d3.v7.min.js"></script>
          <style>
            body {
              background-color: #edf2f8;
            }
            circle {
              fill: #fff;
              stroke: steelblue;
              stroke-width: 1.5px;
            }
            text {
              font: 10px sans-serif;
            }
            path.link {
              fill: none;
              stroke: #ccc;
              stroke-width: 1.5px;
            }
            .clicked circle {
              fill: #313bac;
            }
            #details {
              margin-top: 40px;
              overflow: scroll;
              padding: 10px 10px 0;
              box-shadow: 0 0 20px rgba(0, 0, 0, 0.2);
              border-radius: 15px;
              display: none;
            }
            .json-node {
              margin-left: 20px;
            }
            .json-key {
              font-weight: bold;
              margin-right: 5px;
            }
            .toggle-button {
              margin-right: 5px;
            }
            .json-children {
              display: none;
            }
            .json-node.open .json-children {
              display: block;
            }
          </style>
        </head>
        <body>
          <div id="tree-container">
            <svg id="svg-container"></svg>
            <div id="details"></div>
          </div>
          <script src="https://cdn.rawgit.com/caldwell/renderjson/master/renderjson.js"></script>
          <script>
            const data =
      """
        + jsonData
        + """
            ;
            const width = 1000;
            const marginTop = 10;
            const marginRight = 10;
            const marginBottom = 10;
            const marginLeft = 150;
            // Rows are separated by dx pixels, columns by dy pixels. These names can be counter-intuitive
            // (dx is a height, and dy a width). This because the tree must be viewed with the root at the
            // “bottom”, in the data domain. The width of a column is based on the tree’s height.
            const root = d3.hierarchy(transformDataToHierarchy(data));
            const dx = 20;
            const dy = (width - marginRight - marginLeft) / (1 + root.height);
            // Define the tree layout and the shape for links.
            const tree = d3.tree().nodeSize([dx, dy]);
            const diagonal = d3
              .linkHorizontal()
              .x((d) => d.y)
              .y((d) => d.x);
            // Create the SVG container, a layer for the links and a layer for the nodes.
            const svg = d3
              .select("#svg-container")
              .attr("width", width)
              .attr("height", dx)
              .attr("viewBox", [-marginLeft, -marginTop, width, dx])
              .attr(
                "style",
                "max-width: 100%; height: auto; font: 10px sans-serif; user-select: none;"
              );
            const gLink = svg
              .append("g")
              .attr("fill", "none")
              .attr("stroke", "#555")
              .attr("stroke-opacity", 0.4)
              .attr("stroke-width", 1.5);
            const gNode = svg
              .append("g")
              .attr("cursor", "pointer")
              .attr("pointer-events", "all");
            function update(event, source) {
              const duration = event?.altKey ? 2500 : 250; // hold the alt key to slow down the transition
              const nodes = root.descendants().reverse();
              const links = root.links();
              // Compute the new tree layout.
              tree(root);
              let left = root;
              let right = root;
              root.eachBefore((node) => {
                if (node.x < left.x) left = node;
                if (node.x > right.x) right = node;
              });
              const height = right.x - left.x + marginTop + marginBottom;
              const transition = svg
                .transition()
                .duration(duration)
                .attr("height", height)
                .attr("viewBox", [-marginLeft, left.x - marginTop, width, height])
                .tween(
                  "resize",
                  window.ResizeObserver ? null : () => () => svg.dispatch("toggle")
                );
              // Update the nodes…
              const node = gNode.selectAll("g").data(nodes, (d) => d.id);
              // Enter any new nodes at the parent's previous position.
              const nodeEnter = node
                .enter()
                .append("g")
                .attr("transform", (d) => `translate(${source.y0},${source.x0})`)
                .attr("fill-opacity", 0)
                .attr("stroke-opacity", 0)
                .on("click", (event, d) => {
                  d.children = d.children ? null : d._children;
                  update(event, d);
                  renderDetails(d.data.value);
                  svg.selectAll(".clicked").classed("clicked", false);
                  d3.select(event.currentTarget).classed("clicked", true);
                });
              nodeEnter
                .append("circle")
                .attr("r", 5)
                .attr("fill", (d) => (d._children ? "#555" : "#999"))
                .attr("stroke-width", 10);
              nodeEnter
                .append("text")
                .attr("dy", "0.31em")
                .attr("x", (d) => (d._children ? -6 : 6))
                .attr("text-anchor", (d) => (d._children ? "end" : "start"))
                .text((d) => {
                  const originalText = d.data.name;
                  return originalText.length > 10
                    ? `${originalText.substring(0, d.data.name.indexOf("("))}`
                    : originalText;
                })
                .clone(true)
                .lower()
                .attr("stroke-linejoin", "round")
                .attr("stroke-width", 3)
                .attr("stroke", "white");
              // Transition nodes to their new position.
              const nodeUpdate = node
                .merge(nodeEnter)
                .transition(transition)
                .attr("transform", (d) => `translate(${d.y},${d.x})`)
                .attr("fill-opacity", 1)
                .attr("stroke-opacity", 1);
              // Transition exiting nodes to the parent's new position.
              const nodeExit = node
                .exit()
                .transition(transition)
                .remove()
                .attr("transform", (d) => `translate(${source.y},${source.x})`)
                .attr("fill-opacity", 0)
                .attr("stroke-opacity", 0);
              // Update the links…
              const link = gLink.selectAll("path").data(links, (d) => d.target.id);
              // Enter any new links at the parent's previous position.
              const linkEnter = link
                .enter()
                .append("path")
                .attr("d", (d) => {
                  const o = { x: source.x0, y: source.y0 };
                  return diagonal({ source: o, target: o });
                });
              // Transition links to their new position.
              link.merge(linkEnter).transition(transition).attr("d", diagonal);
              // Transition exiting nodes to the parent's new position.
              link
                .exit()
                .transition(transition)
                .remove()
                .attr("d", (d) => {
                  const o = { x: source.x, y: source.y };
                  return diagonal({ source: o, target: o });
                });
              // Stash the old positions for transition.
              root.eachBefore((d) => {
                d.x0 = d.x;
                d.y0 = d.y;
              });
            }
            // Do the first update to the initial configuration of the tree — where a number of nodes
            // are open (arbitrarily selected as the root, plus nodes with 7 letters).
            root.x0 = dy / 2;
            root.y0 = 0;
            root.descendants().forEach((d, i) => {
              d.id = i;
              d._children = d.children;
              if (d.depth && d.data.name.length !== 7) d.children = null;
            });
            update(null, root);
            //Logic for getting root node
            function valueInsideParentheses(key) {
              const openingIndex = key.indexOf("[");
              const closingIndex = key.lastIndexOf("]");
              const valueInsideParentheses = key.substring(
                openingIndex + 1,
                closingIndex
              );
              return valueInsideParentheses;
            }
            function compareName(parentChainKey, childChainKey) {
              const parentName = parentChainKey.substring(
                0,
                parentChainKey.indexOf("(")
              );
              const childName = childChainKey.substring(
                0,
                childChainKey.indexOf("(")
              );
              if (parentName !== childName) {
                return false;
              }
              return true;
            }
            function compareInputs(parentChainKey, childChainKey) {
              const parentInputs = valueInsideParentheses(parentChainKey);
              const childInputs = valueInsideParentheses(childChainKey);
              return matchInputs(parentInputs, childInputs);
            }
            function compareNameAndInputs(parentChainKey, childChainKey) {
              return (
                compareName(parentChainKey, childChainKey) &&
                compareInputs(parentChainKey, childChainKey)
              );
            }
            function matchInputs(parentInputs, childInputs) {
              let completeMatch = true;
              for (const [parentKey, parentValue] of Object.entries(
                parentInputs
              )) {
                let match = false;
                for (const [childKey, childValue] of Object.entries(
                  childInputs
                )) {
                  if (parentKey === childKey && parentValue === childValue) {
                    match = true;
                    break;
                  }
                }
                if (match === false) {
                  completeMatch = false;
                  break;
                }
              }
              return completeMatch;
            }
            function filterInput(key) {
              const openingIndex = key.indexOf("[");
              const closingIndex = key.lastIndexOf("]");
              const inputValues = key.substring(openingIndex + 1, closingIndex);
              return inputValues;
            }
            function findingRootNode(data) {
              const mySet = new Set();
              Object.keys(data.mainLogicExecInfos).forEach((parentChainKey) => {
                findDependency(parentChainKey, mySet);
              });
              for (const parentChainKey of Object.keys(data.mainLogicExecInfos)) {
                if (!mySet.has(parentChainKey)) {
                  return parentChainKey;
                }
              }
            }
            function findDependency(parentChainkey, mySet) {
              Object.keys(data.mainLogicExecInfos).forEach((childChainKey) => {
                if (!compareNameAndInputs(parentChainkey, childChainKey)) {
                  const mainData = data.mainLogicExecInfos[childChainKey];
                  mainData.dependencyResults &&
                    Object.values(
                      data.mainLogicExecInfos[childChainKey].dependencyResults[0]
                    ).forEach((dependencyValue) => {
                      if (
                        matchInputs(
                          filterInput(parentChainkey),
                          Object.keys(dependencyValue)[0]
                        )
                      ) {
                        mySet.add(parentChainkey);
                      }
                    });
                }
              });
            }
            function buildTree(data, rootNode, rootNodeName) {
              if (
                data.mainLogicExecInfos &&
                data.mainLogicExecInfos[rootNodeName] &&
                data.mainLogicExecInfos[rootNodeName].dependencyResults &&
                data.mainLogicExecInfos[rootNodeName].dependencyResults[0]
              ) {
                Object.values(
                  data.mainLogicExecInfos[rootNodeName].dependencyResults[0]
                ).forEach((dependencyKey) => {
                  if (dependencyKey) {
                    let dependencyKeyName = Object.keys(dependencyKey)[0];
                    if (dependencyKeyName) {
                      let chainNode;
                      Object.keys(data.mainLogicExecInfos).forEach((parentKey) => {
                        if (
                          matchInputs(filterInput(parentKey), dependencyKeyName)\s
                        ) {
                          chainNode = {
                            name: parentKey,
                            children: [],
                            value: JSON.stringify(data.mainLogicExecInfos[parentKey]),
                          };
                          rootNode.children.push(chainNode);
                          buildTree(data, chainNode, parentKey);
                        }
                      });
                    }
                  }
                });
              }
              return rootNode;
            }
            // Transform actual data for d3 Logic
            function transformDataToHierarchy(data) {
              const rootNodeName = findingRootNode(data);
              const rootNode = {
                name: rootNodeName,
                children: [],
                value: JSON.stringify(data.mainLogicExecInfos[rootNodeName]),
              };
              return buildTree(data, rootNode, rootNodeName);
            }
            // Function for rendring Json data in tree format
            function renderJsonData(data) {
              return Object.entries(data)
                .map(([key, value]) => {
                  const isObject = typeof value === "object" && value !== null;
                  const isArray = Array.isArray(value);
                  const hasChildren = isObject && Object.keys(value).length > 0;
                  const toggleButton = hasChildren
                    ? `<button class="toggle-button" onclick="toggle(this)">+</button>`
                    : "";
                  return `
                  <div class="json-node" style="display: flex; gap: 5px">
                    <div class="json-key">${key}: </div>
                    <div class="json-value">
                      ${isObject ? toggleButton : ""}
                      ${isObject && !isArray ? "" : value}
                    </div>
                    <div class="json-children" style="display:\s
                      ${hasChildren ? "none" : "block"
                    }">
                      ${hasChildren && !isArray ? renderJsonData(value) : ""}
                    </div>
                  </div>
                `;
                })
                .join("");
            }
            function toggle(button) {
              const parent = button.parentElement.nextElementSibling;
              const isOpen = parent.style.display === "block";
              parent.style.display = isOpen ? "none" : "block";
              button.textContent = isOpen ? "-" : "+";
            }
            // Function for rendring details box
            function updateInputList(data, map) {
              data.map((item) => {
                for (const key in item) {
                  if (typeof item[key] == "object") {
                    updateInputList([item[key]], map);
                  } else {
                    item[key] = map[item[key]];
                  }
                }
              });
              return data;
            }
            function renderDetails(nodeData) {
              const customBox = document.getElementById("details");
              const formattedData = Object.entries(JSON.parse(nodeData))
                .map(([key, value]) => {
                  let reqValue;
                  if (key === "result") {
                    reqValue = data.dataMap[value];
                  } else if (key === "inputsList") {
                    reqValue = updateInputList(value, data.dataMap);
                  } else if (key === "dependencyResults") {
                    reqValue = updateInputList(value, data.dataMap);
                  } else {
                    reqValue = value;
                  }
                  return `
                      <div style="display: flex; margin-bottom: 8px;">
                          <div style="font-weight: bold; margin-right: 8px;">${key}:</div>
                          <div>
                            ${typeof reqValue == "object"
                              ? renderJsonData(reqValue)
                              : reqValue}
                          </div>
                      </div>
                  `;
                })
                .join("");
              customBox.innerHTML = formattedData;
              customBox.style.display = "block";
            }
          </script>
        </body>
      </html>
    """;
  }
}
