/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.stanag4559.server.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.AbsTime;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.AbsTimeHelper;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.DAG;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Date;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Edge;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Node;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.NodeType;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Time;
import org.jgrapht.Graph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.traverse.CrossComponentIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

public class DAGGenerator {

    /*  Left in as a reference
        List<String> part_nodes = Arrays.asList("NSIL_COMMON",
                "NSIL_COVERAGE",
                "NSIL_CXP",
                "NSIL_EXPLOITATION_INFO",
                "NSIL_GMTI",
                "NSIL_IMAGERY",
                "NSIL_MESSAGE",
                "NSIL_REPORT",
                "NSIL_RFI",
                "NSIL_SDS",
                "NSIL_TASK",
                "NSIL_TDL",
                "NSIL_VIDEO");
    */

    private static final String ORGANIZATION = "DDF";

    private static final String IMAGERY = "IMAGERY";

    // Class Defs
    private static final String STRING_CLASS = "class java.lang.String";

    private static final String INTEGER_CLASS = "class java.lang.Integer";

    private static final String DOUBLE_CLASS = "class java.lang.Double";

    private static final String ABSTIME_CLASS = "class UCO.AbsTime";

    // Entity Fields
    private static final String NSIL_PRODUCT = "NSIL_PRODUCT";

    private static final String NSIL_DESTINATION = "NSIL_DESTINATION";

    private static final String NSIL_ASSOCIATION = "NSIL_ASSOCIATION";

    private static final String NSIL_RELATION = "NSIL_RELATION";

    private static final String NSIL_SOURCE = "NSIL_SOURCE";

    private static final String NSIL_VIDEO = "NSIL_VIDEO";

    private static final String NSIL_MESSAGE = "NSIL_MESSAGE";

    private static final String NSIL_IMAGERY = "NSIL_IMAGERY";

    private static final String NSIL_GMTI = "NSIL_GMTI";

    private static final String NSIL_COMMON = "NSIL_COMMON";

    private static final String NSIL_PART = "NSIL_PART";

    private static final String NSIL_RELATED_FILE = "";

    private static final String NSIL_STREAM = "NSIL_STREAM";

    private static final String NSIL_SECURITY = "NSIL_SECURITY";

    private static final String NSIL_METADATA_SECURITY = "NSIL_METADATA_SECURITY";

    private static final String NSIL_FILE = "NSIL_FILE";

    private static final String NSIL_CARD = "NSIL_CARD";

    private static final String NSIL_APPROVAL = "NSIL_APPROVAL";

    // Attribute Fields
    private static final String IDENTIFIER_UUID = "identifierUUID";

    private static final String TYPE = "type";

    private static final String IDENTIFIER_JOB = "identifierJob";

    private static final String NUMBER_OF_TARGET_REPORTS = "numberOfTargetReports";

    private static final String CATEGORY = "category";

    private static final String RECIPIENT = "recipient";

    private static final String DECOMPRESSION_TECHNIQUE = "decompressionTechnique";

    private static final String NUMBER_OF_BANDS = "numberOfBands";

    private static final String IDENTIFIER = "identifier";

    private static final String MESSAGE_BODY = "messageBody";

    private static final String MESSAGE_TYPE = "messageType";

    private static final String ENCODING_SCHEME = "encodingScheme";

    private static final String PART_IDENTIFIER = "partIdentifier";

    private static final String DATE_TIME_DECLARED = "dateTimeDeclared";

    private static final String PRODUCT_URL = "productUrl";

    private static final String CLASSIFICATION = "classification";

    private static final String POLICY = "policy";

    private static final String RELEASABILITY = "releasability";

    private static final String CREATOR = "creator";

    // Attribute Values
    private static final String XMPP = "XMPP";

    private static final String ENCODING_TYPE = "264ON2";

    private static final String IR = "IR";

    private static final String VIS = "VIS";

    private static final String NC = "NC";

    private static final String UNCLASSIFIED = "UNCLASSIFIED";

    private static final String NATO = "NATO";

    private static final String EU = "EU";

    private static final String NATO_EU = NATO + "/" + EU;

    private static final String PRODUCT_JPG_URL = "http://localhost:20002/data/product.jpg";

    public static int getResultHits() {
        return 1;
    }

    public static DAG[] generateDAGResultNSILAllView(ORB orb) {

        DAG[] metacards = new DAG[1];

        DAG metacard = new DAG();

        Graph<Node, Edge> graph = new DirectedAcyclicGraph<>(Edge.class);

        Node[] nodeRefs = constructNSILProduct(orb, graph, 1);
        constructNSILPart(nodeRefs[0], nodeRefs[1], orb, graph, IMAGERY);
        constructNSILAssociation(nodeRefs[0], nodeRefs[2], orb, graph, 1);

        setUCOEdgeIds(graph);
        setUCOEdges(nodeRefs[0], graph);

        metacard.nodes = getNodeArrayFromGraph(graph);
        metacard.edges = getEdgeArrayFromGraph(graph);
        metacards[0] = metacard;

        return metacards;
    }

    private static Node[] getNodeArrayFromGraph(Graph<Node, Edge> graph) {
        Object[] vertexSet = graph.vertexSet()
                .toArray();

        Node[] result = new Node[vertexSet.length];

        for (int i = 0; i < vertexSet.length; i++) {
            result[i] = (Node) vertexSet[i];
        }

        return result;
    }

    private static Edge[] getEdgeArrayFromGraph(Graph<Node, Edge> graph) {
        Object[] edgeSet = graph.edgeSet()
                .toArray();

        Edge[] result = new Edge[edgeSet.length];

        for (int i = 0; i < edgeSet.length; i++) {
            result[i] = (Edge) edgeSet[i];
            result[i].relationship_type = "";
        }
        return result;
    }

    /**
     * Set the UCO.Node IDs in DFS order to conform to the STANAG 4459 spec.  The root of the node
     * will be 0.
     *
     * @param graph - the graph representation of the DAG
     */
    private static void setUCOEdgeIds(Graph<Node, Edge> graph) {
        int id = 0;
        CrossComponentIterator<Node, Edge, Boolean> depthFirstIterator = new DepthFirstIterator(
                graph);
        while (depthFirstIterator.hasNext()) {
            Node node = depthFirstIterator.next();
            node.id = id;
            id++;
        }
    }

    /**
     * Set the UCO.Edges of the DAG according to the STANAG 4459 Spec.  This requires the ids
     * of the Nodes to be set in DFS order.
     *
     * @param root  - the root node of the graph (NSIL_PRODUCT)
     * @param graph - the graph representation of the DAG
     */
    private static void setUCOEdges(Node root, Graph<Node, Edge> graph) {
        Stack<Node> stack = new Stack<>();
        Stack<Node> visitorStack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node currNode = stack.pop();
            if (!visitorStack.contains(currNode)) {
                visitorStack.push(currNode);
                for (Edge edge : graph.edgesOf(currNode)) {

                    Node source = graph.getEdgeSource(edge);
                    Node target = graph.getEdgeTarget(edge);

                    // Remove if statement?
                    if (edge != null && source != null && target != null) {
                        edge.start_node = source.id;
                        edge.end_node = target.id;
                        stack.push(target);
                    }
                }
            }
        }
    }

    /**
     * Constructs the NSIL_PRODUCT subgraph of the NSIL_ALL_VIEW.  This method sets builds the NSIL_PRODUCT
     * with all optional nodes ( NSIL_APPROVAL, etc.) as well as all MANDATORY attributes for these NODES
     * according to the STANAG 4459 spec.
     *
     * @param orb            - a reference to the orb to create UCO objects
     * @param graph          - the graph representation of the DAG
     * @param numRelatedFile - the number of NSIL_RELATED_FILE's to create.  This number is unbounded
     *                       according to the specification.
     * @return a Node[] that contains a reference to the root, NSIL_SECURITY, and NSIL_CARD that are used
     * in other subgraphs.
     */
    private static Node[] constructNSILProduct(ORB orb, Graph<Node, Edge> graph,
            int numRelatedFile) {
        List<String> product_nodes = Arrays.asList(NSIL_APPROVAL,
                NSIL_FILE,
                NSIL_STREAM,
                NSIL_METADATA_SECURITY,
                NSIL_CARD,
                NSIL_SECURITY);
        List<Node> nodeProductNodes = getEntityListFromStringList(product_nodes, orb);

        Node[] nodeArray = new Node[3];

        Node root = constructRootNode(orb);
        nodeArray[0] = root;
        graph.addVertex(root);
        Node attribute;

        for (Node node : nodeProductNodes) {
            graph.addVertex(node);
            graph.addEdge(root, node);

            if (node.attribute_name.equals(NSIL_SECURITY)) {
                nodeArray[1] = node;
            } else if (node.attribute_name.equals(NSIL_CARD)) {
                nodeArray[2] = node;
            }

            switch (node.attribute_name) {
            case NSIL_FILE:
                attribute = constructAttributeNode(CREATOR, ORGANIZATION, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(DATE_TIME_DECLARED,
                        new AbsTime(new Date((short) 2, (short) 10, (short) 16),
                                new Time((short) 10, (short) 0, (short) 0)),
                        orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(PRODUCT_URL, PRODUCT_JPG_URL, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                break;

            case NSIL_METADATA_SECURITY:
                attribute = constructAttributeNode(CLASSIFICATION, UNCLASSIFIED, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(POLICY, NATO_EU, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(RELEASABILITY, NATO, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                break;

            case NSIL_SECURITY:
                attribute = constructAttributeNode(CLASSIFICATION, UNCLASSIFIED, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(POLICY, NATO_EU, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(RELEASABILITY, NATO, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                break;

            case NSIL_STREAM:
                attribute = constructAttributeNode(CREATOR, ORGANIZATION, orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                attribute = constructAttributeNode(DATE_TIME_DECLARED,
                        new AbsTime(new Date((short) 2, (short) 10, (short) 16),
                                new Time((short) 10, (short) 0, (short) 0)),
                        orb);
                graph.addVertex(attribute);
                graph.addEdge(node, attribute);
                break;
            }

        }

        for (int i = 0; i < numRelatedFile; i++) {
            Node node = constructEntityNode(NSIL_RELATED_FILE, orb);
            graph.addVertex(node);
            graph.addEdge(root, node);

            attribute = constructAttributeNode(CREATOR, ORGANIZATION, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(DATE_TIME_DECLARED, new AbsTime(new Date((short) 2,
                    (short) 10,
                    (short) 16), new Time((short) 10, (short) 0, (short) 0)), orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
        }
        return nodeArray;
    }

    /**
     * Constructs a NSIL_PART with all optional nodes, and all mandatory attributes of those nodes according to the STANAG 4459 spec.
     * A NISL_PRODUCT in NSIL_ALL_VIEW can contain 0...n NSIL_PARTS.  A NISL_PART will have an edge pointing to NSIL_SECURITY
     *
     * @param nsilProduct  - a reference to the root node to link to the DAG graph
     * @param nsilSecurity - a reference to NSIL_SECURITY to link to the NSIL_PART subgraph
     * @param orb          - a reference to the orb to create UCO objects
     * @param graph        - the graph representation of the DAG
     */
    private static void constructNSILPart(Node nsilProduct, Node nsilSecurity, ORB orb,
            Graph<Node, Edge> graph, String partType) {

        Node root = constructEntityNode(NSIL_PART, orb);
        graph.addVertex(root);
        graph.addEdge(nsilProduct, root);
        graph.addEdge(root, nsilSecurity);

        Node attribute;
        attribute = constructAttributeNode(PART_IDENTIFIER, "", orb);
        graph.addVertex(attribute);
        graph.addEdge(root, attribute);

        Node node = constructEntityNode(partType, orb);
        graph.addVertex(node);
        graph.addEdge(root, node);

        switch (partType) {
        case NSIL_COMMON:
            attribute = constructAttributeNode(IDENTIFIER_UUID, new UUID(0, 100).toString(), orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(TYPE, IMAGERY, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            break;

        case NSIL_GMTI:
            attribute = constructAttributeNode(IDENTIFIER_JOB, 123.1, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(NUMBER_OF_TARGET_REPORTS, 1, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            break;

        case NSIL_IMAGERY:
            attribute = constructAttributeNode(CATEGORY, VIS, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(DECOMPRESSION_TECHNIQUE, NC, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(IDENTIFIER, ORGANIZATION + "1", orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(NUMBER_OF_BANDS, 1, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            break;

        case NSIL_MESSAGE:
            attribute = constructAttributeNode(RECIPIENT, ORGANIZATION + "2", orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(MESSAGE_BODY, "This is a message", orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(MESSAGE_TYPE, XMPP, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            break;

        case NSIL_VIDEO:
            attribute = constructAttributeNode(CATEGORY, IR, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            attribute = constructAttributeNode(ENCODING_SCHEME, ENCODING_TYPE, orb);
            graph.addVertex(attribute);
            graph.addEdge(node, attribute);
            break;
        }
    }

    /**
     * Constructs a NSIL_ASSOCIATION subgraph with all optional nodes, as well as all mandatory attributes
     * for these nodes.  A NSIL_PRODUCT can contain 0...n NSIL_ASSOCIATIONS.  All NSIL_ASSOCIATIONS contain
     * an edge to the NSIL_CARD node.  A NSIL_ASSOCIATION can contain 1...n NSIL_DESTINATIONS.
     *
     * @param nsilProduct     - a reference to the root node to link to the DAG graph
     * @param nsilCard        - a reference to the NSIL_CARD to link to the NSIL_ASSOCIATION subgraph
     * @param orb             - a reference to the orb to create UCO objects
     * @param graph           - the graph representation of the DAG
     * @param numDestinations - the number of NSIL_DESTINATION nodes to create
     */
    private static void constructNSILAssociation(Node nsilProduct, Node nsilCard, ORB orb,
            Graph<Node, Edge> graph, int numDestinations) {

        List<String> association_nodes = Arrays.asList(NSIL_RELATION, NSIL_SOURCE);
        List<Node> nodePartNodes = getEntityListFromStringList(association_nodes, orb);

        Node root = constructEntityNode(NSIL_ASSOCIATION, orb);
        graph.addVertex(root);
        graph.addEdge(nsilProduct, root);
        graph.addEdge(root, nsilCard);

        for (Node n : nodePartNodes) {
            graph.addVertex(n);
        }

        graph.addEdge(root, nodePartNodes.get(1));

        graph.addEdge(nodePartNodes.get(1), nsilCard);

        for (int i = 0; i < numDestinations; i++) {
            Node nsilDestination = constructEntityNode(NSIL_DESTINATION, orb);
            graph.addVertex(nsilDestination);
            graph.addEdge(root, nsilDestination);
            graph.addEdge(nsilDestination, nsilCard);
        }
    }

    private static List<Node> getEntityListFromStringList(List<String> list, ORB orb) {
        List<Node> nodeList = new ArrayList<>();
        for (String string : list) {
            nodeList.add(constructEntityNode(string, orb));
        }
        return nodeList;
    }

    /*
        Construction methods use 0 as the node identifier and are set later according to
        the graph structure.
     */
    private static Node constructRootNode(ORB orb) {
        return new Node(0, NodeType.ROOT_NODE, NSIL_PRODUCT, orb.create_any());
    }

    private static Node constructEntityNode(String entityName, ORB orb) {
        return new Node(0, NodeType.ENTITY_NODE, entityName, orb.create_any());
    }

    private static Node constructAttributeNode(String attributeName, Object attributeValues,
            ORB orb) {
        Any any = orb.create_any();

        switch (attributeValues.getClass()
                .toString()) {
        case STRING_CLASS:
            any.insert_string((String) attributeValues);
            break;

        case INTEGER_CLASS:
            any.insert_ulong((int) attributeValues);
            break;

        case DOUBLE_CLASS:
            any.insert_double((double) attributeValues);
            break;

        case ABSTIME_CLASS:
            AbsTimeHelper.insert(any, (AbsTime) attributeValues);
            break;

        default:
            break;
        }
        return new Node(0, NodeType.ATTRIBUTE_NODE, attributeName, any);
    }
}
