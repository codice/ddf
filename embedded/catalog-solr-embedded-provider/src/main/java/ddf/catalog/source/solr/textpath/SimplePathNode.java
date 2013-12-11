/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.source.solr.textpath;

import org.parboiled.trees.ImmutableBinaryTreeNode;

public class SimplePathNode extends ImmutableBinaryTreeNode<SimplePathNode> {

    public enum NodeType {
        TEXT_PATH, DFS, FS, DOT, STAR, STEP_PATH, ABSOLUTE_PATH, RELATIVE_PATH, PREDICATE, NCNAME, VALUE
    };

    private NodeType type;

    private String value;

    // Do not use this directly
    protected SimplePathNode(SimplePathNode left, SimplePathNode right) {
        super(left, right);
    }

    public SimplePathNode(String value) {
        super(null, null);
        this.type = NodeType.VALUE;
        this.value = value;
    }

    public SimplePathNode(NodeType type, SimplePathNode left, SimplePathNode right) {
        super(left, right);
        this.type = type;
    }

    public String getValue(String searchPhrase) {
        if (type.equals(NodeType.TEXT_PATH)) {
            return "/" + left().getValue() + "\\|" + searchPhrase + "/";
        } else {
            throw new IllegalArgumentException(
                    "Can only getValue with search phrase on a TextPath node type.");
        }
    }

    public String getValue() {
        switch (type) {
        case TEXT_PATH:
            return "/" + left().getValue() + "[\\/|\\|].*[\\/|\\|]?.*/";
        case DFS:
            return "\\/.*\\/?" + left().getValue();
        case FS:
            return "\\/" + left().getValue();
        case DOT:
        case ABSOLUTE_PATH:
            return "";
        case STAR:
            return "[^\\/]*";
        case STEP_PATH:
            return "\\/?" + left().getValue();
        case RELATIVE_PATH:
            return left().getValue() + right().getValue();
        case PREDICATE:
            return left().getValue() + "\\/?" + right().getValue();
        case NCNAME:
            return left().getValue();
        case VALUE:
            return value;
        default:
            throw new IllegalStateException();
        }
    }

    public static NodeType getNodeType(String match) {
        return match.equals("/") ? NodeType.FS : NodeType.DFS;
    }

    @Override
    public String toString() {
        return "Type:[" + this.type + "] value:[" + this.value + "] [" + left() + "],[" + right()
                + "]";
    }

}
