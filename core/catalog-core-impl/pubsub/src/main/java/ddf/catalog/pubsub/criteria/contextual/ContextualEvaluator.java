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

package ddf.catalog.pubsub.criteria.contextual;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ddf.util.XPathHelper;

public final class ContextualEvaluator {
    private static final String FIELD_NAME = "Resource";

    private static final String CASE_SENSITIVE_FIELD_NAME = "cs_Resource";

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(ContextualEvaluator.class));

    private static final String DEFAULT_XPATH_1 = "/*[local-name()=\"Resource\"]/*"
            + "[local-name() != \"identifier\" and " + "local-name() != \"language\" and "
            + "local-name() != \"dates\" and " + "local-name() != \"rights\" and "
            + "local-name() != \"format\" and " + "local-name() != \"subjectCoverage\" and "
            + "local-name() != \"temporalCoverage\" and "
            + "local-name() != \"geospatialCoverage\"  " + "] ";

    private static final String DEFAULT_XPATH_2 = "/*[local-name()=\"Resource\"]"
            + "/*[local-name()=\"geospatialCoverage\"]/*[local-name()=\"GeospatialExtent\"]"
            + "/*[not(ancestor::node()[local-name()=\"boundingGeometry\"] or descendant-or-self::node()[local-name()=\"boundingGeometry\"])] ";

    private static final String[] DEFAULT_XPATH_SELECTORS = new String[] {DEFAULT_XPATH_1,
        DEFAULT_XPATH_2};

    private ContextualEvaluator() {
        throw new UnsupportedOperationException(
                "This is a utility class - it should never be instantiated");
    }

    /**
     * @param cec
     * 
     * @return
     * 
     * @throws IOException
     * @throws ParseException
     */
    public static boolean evaluate(ContextualEvaluationCriteria cec) throws IOException,
        ParseException {
        String methodName = "evaluate";
        logger.entry(methodName);

        Directory index = cec.getIndex();
        String searchPhrase = cec.getCriteria();

        // Handle case where no search phrase is specified. Contextual criteria should then specify
        // text path(s)
        // and be used to determine if an element or attribute exist
        if (searchPhrase == null || searchPhrase.isEmpty()) {
            String[] textPaths = cec.getTextPaths();
            String fullDocument = cec.getMetadata();

            if (textPaths != null && textPaths.length > 0 && fullDocument != null) {
                String indexableText = getIndexableText(fullDocument, textPaths);
                if (indexableText != null && !indexableText.isEmpty()) {
                    logger.trace("Found element/attribute for textPaths");
                    logger.exit(methodName + " - returning true");
                    return true;
                }
            }

            logger.trace("No search phrase specified and could not find element/attribute based on textPaths");
            logger.exit(methodName + " - returning false");
            return false;
        }

        // a. query
        QueryParser queryParser = null;
        if (cec.isCaseSensitiveSearch()) {
            logger.debug("Doing case-sensitive search ...");
            queryParser = new QueryParser(Version.LUCENE_30, CASE_SENSITIVE_FIELD_NAME,
                    new CaseSensitiveContextualAnalyzer(Version.LUCENE_30));

            // Make Wildcard, Prefix, Fuzzy, and Range queries *not* be automatically lower-cased,
            // i.e., make them be case-sensitive
            queryParser.setLowercaseExpandedTerms(false);
        } else {
            logger.debug("Doing case-insensitive search ...");
            queryParser = new QueryParser(Version.LUCENE_30, FIELD_NAME, new ContextualAnalyzer(
                    Version.LUCENE_30));
        }

        // Configures Lucene query parser to allow a wildcard as first character in the
        // contextual search phrase
        queryParser.setAllowLeadingWildcard(true);

        Query q = queryParser.parse(searchPhrase);

        // b. search
        int hitsPerPage = 1;
        IndexSearcher searcher = new IndexSearcher(index, true);
        TopDocs topDocs = searcher.search(q, hitsPerPage);

        // c. display results
        logger.debug("Found " + topDocs.totalHits + " hits.");

        // searcher can only be closed when there
        // is no need to access the documents any more.
        searcher.close();

        logger.exit(methodName);

        return topDocs.totalHits > 0;
    }

    /**
     * Create a field with the specified field name and value, and add it to a Lucene Document to be
     * added to the specified IndexWriter.
     * 
     * @param indexWriter
     * @param fieldName
     * @param value
     * 
     * @throws IOException
     */
    private static void addDoc(IndexWriter indexWriter, String fieldName, String value)
        throws IOException {
        Document doc = new Document();
        doc.add(new Field(fieldName, value, Field.Store.YES, Field.Index.ANALYZED,
                Field.TermVector.WITH_POSITIONS_OFFSETS));
        indexWriter.addDocument(doc);
    }

    /**
     * Build one Lucene index for the specified XML Document that contains both case-insensitive and
     * case-sensitive indexed text. Use the default XPath selectors to extract the indexable text
     * from the specified XML document.
     * 
     * @param fullDocument
     *            the XML document to be indexed
     * 
     * @return the Lucene index for the indexed text from the XML document
     * 
     * @throws IOException
     */
    public static Directory buildIndex(String fullDocument) throws IOException {
        String methodName = "buildIndex (DEFAULT)";
        logger.entry(methodName);

        return buildIndex(fullDocument, DEFAULT_XPATH_SELECTORS);
    }

    /**
     * Build one Lucene index for the specified XML Document that contains both case-insensitive and
     * case-sensitive indexed text. Use the provided XPath selectors to extract the indexable text
     * from the specified XML document.
     * 
     * @param fullDocument
     *            the XML document to be indexed
     * @param xpathSelectors
     *            the XPath selectors to use to extract the indexable text from the XML document
     * 
     * @return the Lucene index for the indexed text from the XML document
     * 
     * @throws IOException
     */
    public static Directory buildIndex(String fullDocument, String[] xpathSelectors)
        throws IOException {
        String methodName = "buildIndex";
        logger.entry(methodName);

        // logger.debug( XPathHelper.xmlToString( fullDocument ) );

        // 0. Specify the analyzer for tokenizing text.
        // The same analyzer should be used for indexing and searching
//        StandardAnalyzer standardAnalyzer = new StandardAnalyzer(Version.LUCENE_30);
        ContextualAnalyzer contextualAnalyzer = new ContextualAnalyzer(Version.LUCENE_30);
        CaseSensitiveContextualAnalyzer caseSensitiveStandardAnalyzer = new CaseSensitiveContextualAnalyzer(
                Version.LUCENE_30);

        // 1. create the index
        Directory index = new RAMDirectory();

        // Retrieve the text from the document that can be indexed using the specified XPath
        // selectors
        String indexableText = getIndexableText(fullDocument, xpathSelectors);

        // Create an IndexWriter using the case-insensitive StandardAnalyzer
        // NOTE: the boolean arg in the IndexWriter constructor means to create a new index,
        // overwriting any existing index
//        IndexWriter indexWriter = new IndexWriter(index, standardAnalyzer, true,
//                IndexWriter.MaxFieldLength.UNLIMITED);
        IndexWriter indexWriter = new IndexWriter(index, contextualAnalyzer, true,
                IndexWriter.MaxFieldLength.UNLIMITED);
        logTokens(indexWriter.getAnalyzer(), FIELD_NAME, fullDocument, "ContextualAnalyzer");

        // Add the indexable text to the case-insensitive index writer, assigning it the
        // "case-insensitive" field name
        addDoc(indexWriter, FIELD_NAME, indexableText);
        indexWriter.close();

        // Create a second IndexWriter using the custom case-sensitive StandardAnalyzer
        // NOTE: set boolean to false to append the case-sensitive indexed text to the existing
        // index (populated by first IndexWriter)
        IndexWriter csIndexWriter = new IndexWriter(index, caseSensitiveStandardAnalyzer, false,
                IndexWriter.MaxFieldLength.UNLIMITED);

        // Add the indexable text to the case-sensitive index writer, assigning it the
        // "case-sensitive" field name
        addDoc(csIndexWriter, CASE_SENSITIVE_FIELD_NAME, indexableText);
        csIndexWriter.close();

        logger.exit(methodName);

        return index;
    }
    
    private static void logTokens(Analyzer analyzer, String fieldName, String fullDocument, String analyzerName) throws IOException {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(fullDocument));
        OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
        TermAttribute termAttribute = tokenStream.getAttribute(TermAttribute.class);
        logger.debug("-----  {} tokens  -----", analyzerName);
        while (tokenStream.incrementToken()) {
            int startOffset = offsetAttribute.startOffset();
            int endOffset = offsetAttribute.endOffset();
            String term = termAttribute.term();
            logger.debug(term);
        }
        logger.debug("-----  END:  {} tokens  -----", analyzerName);
    }
    
    /**
     * Extract the text from the specified XML Document that is to be indexed using the specified
     * XPath selectors.
     * 
     * @param document
     * @param xpathSelectors
     * @return
     */
    private static String getIndexableText(String document, String[] xpathSelectors) {
        String methodName = "getIndexableText";
        logger.entry(methodName);

        List<String> indexedText = new ArrayList<String>();

        // logger.debug( XPathHelper.xmlToString( document ) );

        logger.debug("xpathSelectors.size = " + xpathSelectors.length);

        StringBuilder sbuilder = new StringBuilder();

        try {
            // logger.trace( "BEFORE - xmlString:\n" + document );

            // TODO Is this safe for all cases? Can there be multiple default namespaces such that
            // this would screw up the metadata?

            // Treat the "default namespace" (i.e., xmlns="http://some.namespace") the same as the
            // "no namespace" (i.e., xmlns="")
            // so that user-specified XPath Selectors do not need to specify a namespace for
            // expressions in the default namespace
            // (For example, user can specify //fileTitle vs. //namespace:fileTitle, where a
            // NamespaceContext/NamespaceResolver
            // would try to resolve the namespace they specified)
            // The regex below, "xmlns=['\"].*?['\"]", looks for:
            // xmlns="any chars between single or double quotes"

            document = document.replaceAll("xmlns=['\"].*?['\"]", "");
            // logger.trace( "AFTER - xmlString:\n" + document );

            XPathHelper xHelper = new XPathHelper(document);

            for (String xpath : xpathSelectors) {
                logger.debug("Processing xpath selector:\n" + xpath);
                // NodeList nodeList = (NodeList) xHelper.evaluate( xpath, XPathConstants.NODESET,
                // new NamespaceResolver() );
                NodeList nodeList = (NodeList) xHelper.evaluate(xpath, XPathConstants.NODESET);
                logger.debug("nodeList length = " + nodeList.getLength());

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                        Attr attribute = (Attr) node;
                        logger.debug("Adding text [" + attribute.getNodeValue() + "]");

                        sbuilder.append(attribute.getNodeValue() + " ");
                    }

                    // On each element node detected, traverse all of its children. Look for
                    // any Text nodes it has, adding their text values to the list of indexable text
                    else if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element elem = (Element) node;
                        // logger.trace( "Element node " + elem.getNodeName() + " detected" );
                        traverse(elem, indexedText);

                        // getTextContent() concatenates *all* text from all descendant Text nodes
                        // without
                        // any white space between each Text node's value, e.g., JohnDoe vs. John
                        // Doe
                        // That's not good ...
                        // logger.debug( "Adding text [" + elem.getTextContent() + "]" );
                        //
                        // sbuilder.append( elem.getTextContent() + " " );
                    }

                    else {
                        logger.debug("Unsupported node type: " + node.getNodeType()
                                + ",   node name = " + node.getNodeName());
                    }
                }
            }
        } catch (XPathExpressionException e1) {
            logger.catching(e1);
        }

        // Append all of the Text nodes' values to the single indexable text string
        for (String text : indexedText) {
            sbuilder.append(text);
        }

        // logger.debug( "Indexable Text: " + sbuilder.toString() );

        logger.exit(methodName);

        return sbuilder.toString();
    }

    private static void traverse(Node n, List<String> indexedText) {
        // logger.trace( "Node: " + n.getNodeName() + "test=[" + n.getNodeValue() + "]" );

        // Traverse the rest of the tree in depth-first order.
        if (n.getNodeType() == Node.TEXT_NODE) {
            indexedText.add(n.getNodeValue() + " ");
        }

        if (n.hasChildNodes()) {
            // Get the children in a list.
            NodeList nl = n.getChildNodes();

            for (int i = 0; i < nl.getLength(); i++) {
                // Recursively traverse each of the children.
                traverse(nl.item(i), indexedText);
            }
        }
    }

}
