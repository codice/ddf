/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.pubsub.criteria.contextual;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.util.Version;

/**
 * Filters {@link org.apache.lucene.analysis.standard.StandardTokenizer} with {@link
 * StandardFilter}, {@link LowerCaseFilter} and {@link StopFilter}, using a list of English stop
 * words.
 *
 * <p><a name="version"/>
 *
 * <p>You must specify the required {@link Version} compatibility when creating StandardAnalyzer:
 *
 * <ul>
 *   <li>As of 2.9, StopFilter preserves position increments
 *   <li>As of 2.4, Tokens incorrectly identified as acronyms are corrected (see <a
 *       href="https://issues.apache.org/jira/browse/LUCENE-1068">LUCENE-1608</a>
 * </ul>
 */
public class CaseSensitiveContextualAnalyzer extends Analyzer {
  /**
   * An unmodifiable set containing some common English words that are usually not useful for
   * searching.
   */
  protected static final Set<?> STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

  /** Default maximum allowed token length */
  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

  /**
   * Specifies whether deprecated acronyms should be replaced with HOST type. See {@linkplain
   * https://issues.apache.org/jira/browse/LUCENE-1068}
   */
  private final boolean replaceInvalidAcronym, enableStopPositionIncrements;

  private final Version matchVersion;

  private Set<?> stopSet;

  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

  /**
   * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
   *
   * @param matchVersion Lucene version to match See {@link <a href="#version">above</a>}
   */
  public CaseSensitiveContextualAnalyzer(Version matchVersion) {
    this(matchVersion, STOP_WORDS_SET);
  }

  /**
   * Builds an analyzer with the given stop words.
   *
   * @param matchVersion Lucene version to match See {@link <a href="#version">above</a>}
   * @param stopWords stop words
   */
  public CaseSensitiveContextualAnalyzer(Version matchVersion, Set<?> stopWords) {
    stopSet = stopWords;
    setOverridesTokenStreamMethod(CaseSensitiveContextualAnalyzer.class);
    enableStopPositionIncrements =
        StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion);
    replaceInvalidAcronym = matchVersion.onOrAfter(Version.LUCENE_24);
    this.matchVersion = matchVersion;
  }

  /**
   * Builds an analyzer with the stop words from the given file.
   *
   * @see WordlistLoader#getWordSet(File)
   * @param matchVersion Lucene version to match See {@link <a href="#version">above</a>}
   * @param stopwords File to read stop words from
   */
  public CaseSensitiveContextualAnalyzer(Version matchVersion, File stopwords) throws IOException {
    this(matchVersion, WordlistLoader.getWordSet(stopwords));
  }

  /**
   * Builds an analyzer with the stop words from the given reader.
   *
   * @see WordlistLoader#getWordSet(Reader)
   * @param matchVersion Lucene version to match See {@link <a href="#version">above</a>}
   * @param stopwords Reader to read stop words from
   */
  public CaseSensitiveContextualAnalyzer(Version matchVersion, Reader stopwords)
      throws IOException {
    this(matchVersion, WordlistLoader.getWordSet(stopwords));
  }

  /**
   * Constructs a {@link org.apache.lucene.analysis.standard.StandardTokenizer} filtered by a {@link
   * StandardFilter}, a {@link LowerCaseFilter} and a {@link StopFilter}.
   */
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    ContextualTokenizer tokenStream = new ContextualTokenizer(reader);
    TokenStream result = new StandardFilter(tokenStream);
    result = new StopFilter(enableStopPositionIncrements, result, stopSet);
    return result;
  }

  /** @see #setMaxTokenLength */
  public int getMaxTokenLength() {
    return maxTokenLength;
  }

  /**
   * Set maximum allowed token length. If a token is seen that exceeds this length then it is
   * discarded. This setting only takes effect the next time tokenStream or reusableTokenStream is
   * called.
   */
  public void setMaxTokenLength(int length) {
    maxTokenLength = length;
  }

  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    if (overridesTokenStreamMethod) {
      // LUCENE-1678: force fallback to tokenStream() if we
      // have been subclassed and that subclass overrides
      // tokenStream but not reusableTokenStream
      return tokenStream(fieldName, reader);
    }
    SavedStreams streams = (SavedStreams) getPreviousTokenStream();
    if (streams == null) {
      streams = new SavedStreams();
      setPreviousTokenStream(streams);
      streams.tokenStream = new ContextualTokenizer(reader);
      streams.filteredTokenStream = new StandardFilter(streams.tokenStream);
      streams.filteredTokenStream =
          new StopFilter(enableStopPositionIncrements, streams.filteredTokenStream, stopSet);
    } else {
      streams.tokenStream.reset(reader);
    }

    return streams.filteredTokenStream;
  }

  private static final class SavedStreams {
    ContextualTokenizer tokenStream;

    TokenStream filteredTokenStream;
  }
}
