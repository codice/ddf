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

package org.codice.ddf.commands.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;

import org.codice.ddf.commands.util.CrossPlatformFilePathEvaluator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CrossPlatformFilePathEvaluatorTest {
    String docsDirPath;

    String downloadsDirPath;

    String downDirPath;

    String ideasDirPath;

    final String sampleA = "sampleA.txt";

    final String sampleB = "sampleB.png";

    final String sampleC = "sampleC.jpg";

    final String sampleD = "sampleD.txt";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        docsDirPath = setupDirWithSampleFiles("Documents");
        downloadsDirPath = setupDirWithSampleFiles("downloads");
        downDirPath = setupDirWithSampleFiles("down");
        ideasDirPath = setupDirWithSampleFiles("brilliantIdeas");
    }

    private String setupDirWithSampleFiles(String dirName) throws IOException {
        File dir = temporaryFolder.newFolder(dirName);
        new File(dir.getAbsolutePath() + sampleA).createNewFile();
        new File(dir.getAbsolutePath() + sampleB).createNewFile();
        new File(dir.getAbsolutePath() + sampleC).createNewFile();
        new File(dir.getAbsolutePath() + sampleD).createNewFile();
        return dir.getAbsolutePath();
    }

    @Test
    public void testAbsolutePath() throws Exception {
        String path = docsDirPath.replaceAll("\\\\", "") + sampleA;
        File file = CrossPlatformFilePathEvaluator.handlePath(path);
        assertThat(file.exists(), is(true));
    }

    @Test
    public void testAbsolutePathSimilarFolderName() throws Exception {
        String path = downloadsDirPath.replaceAll("\\\\", "") + sampleB;
        File file = CrossPlatformFilePathEvaluator.handlePath(path);
        assertThat(file.getAbsolutePath(), equals(downDirPath + "\\" + sampleB));
    }
}
