package etomica.graph.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import etomica.graph.model.Edge;
import etomica.graph.model.Graph;
import etomica.graph.model.GraphFactory;
import etomica.graph.model.Node;
import static etomica.graph.model.Metadata.*;

public class Mul implements Binary {

  public Set<Graph> apply(Set<Graph> left, Set<Graph> right, Parameters params) {

    Unary isoFree = new IsoFree();
    Set<Graph> result = new HashSet<Graph>();
    for (Graph lg : left) {
      for (Graph rg : right) {
        Graph graph = apply(lg, rg);
        if (graph != null) {
          result.add(graph);
        }
      }
    }
    return isoFree.apply(result, params);
  }

  public Graph apply(Graph left, Graph right) {

    List<Byte> sameLabelRootNodes = new ArrayList<Byte>();
    // two root nodes with the same nodeId in left and right must have the same color
    for (byte nodeId = 0; nodeId < left.nodeCount(); nodeId++) {
      if (nodeId >= right.nodeCount()) {
        break;
      }
      Node leftNode = left.nodes().get(nodeId);
      Node rightNode = right.nodes().get(nodeId);
      if (leftNode.getType() == TYPE_NODE_ROOT && rightNode.getType() == TYPE_NODE_ROOT) {
        if (!leftNode.isSameColor(rightNode)) {
          return null;
        }
        sameLabelRootNodes.add(nodeId);
      }
    }
    // left and right must not have edges connecting root nodes with the same labels
    for (byte fromNode = 0; fromNode < left.nodeCount(); fromNode++) {
      if (fromNode >= right.nodeCount()) {
        break;
      }
      if (left.nodes().get(fromNode).getType() != TYPE_NODE_ROOT) {
        continue;
      }
      for (byte toNode = (byte) (fromNode + 1); toNode < left.nodeCount(); toNode++) {
        if (toNode >= right.nodeCount()) {
          break;
        }
        if (left.nodes().get(fromNode).getType() != TYPE_NODE_ROOT) {
          continue;
        }
        if (left.hasEdge(fromNode, toNode) && left.hasEdge(fromNode, toNode)) {
          return null;
        }
      }
    }
    // union of nodes : all nodes from left
    Node[] nodes = new Node[left.nodes().size() + right.nodes().size() - sameLabelRootNodes.size()];
    for (byte nodeId = 0; nodeId < left.nodes().size(); nodeId++) {
      nodes[nodeId] = left.nodes().get(nodeId).copy();
    }
    // union of nodes : nodes from right except those with a corresponding root node in
    // left;
    // nodes from right are relabeled in the result
    byte nodeIndex = (byte) left.nodes().size();
    for (byte nodeId = 0; nodeId < right.nodes().size(); nodeId++) {
      if (sameLabelRootNodes.get(nodeId) != null) {
        continue;
      }
      Node rnode = right.nodes().get(nodeId);
      nodes[nodeIndex] = GraphFactory.createNode(nodeIndex, rnode.getColor(), rnode.getType());
      nodeIndex++;
    }
    // create the graph
    Graph result = GraphFactory.createGraph(nodes);
    // union of edges : all edges from left
    for (Edge edge : left.edges()) {
      Edge newEdge = result.putEdge(left.getFromNode(edge.getId()), left.getToNode(edge.getId()));
      newEdge.setColor(edge.getColor());
    }
    // union of edges : all edges from right; adjust for the relabeled nodes from right
    byte deltaId = (byte) left.nodes().size();
    for (Edge edge : right.edges()) {
      byte fromNode = right.getFromNode(edge.getId());
      if (sameLabelRootNodes.get(fromNode) == null) {
        fromNode = (byte) (fromNode + deltaId);
      }
      byte toNode = right.getToNode(edge.getId());
      if (sameLabelRootNodes.get(toNode) == null) {
        fromNode = (byte) (toNode + deltaId);
      }
      Edge newEdge = result.putEdge(fromNode, toNode);
      newEdge.setColor(edge.getColor());
    }
    // update the coefficient (default value is 1)
    result.coefficient().multiply(left.coefficient());
    result.coefficient().multiply(right.coefficient());
    result.setNumFactors(left.factors().length);
    result.addFactors(left.factors());
    result.addFactors(right.factors());
    return result;
  }
}