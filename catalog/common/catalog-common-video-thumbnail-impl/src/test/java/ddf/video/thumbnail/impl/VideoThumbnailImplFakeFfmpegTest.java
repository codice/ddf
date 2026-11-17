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
package ddf.video.thumbnail.impl;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Uses a fake ffmpeg script so the ffmpegPath config and error handling get tested on any arch.
 * {@link VideoThumbnailImplTest} covers the real bundled ffmpeg but only runs on x86.
 */
public class VideoThumbnailImplFakeFfmpegTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private String originalDdfHome;

  private File ddfHome;

  private File invocationLog;

  private File videoFile;

  private VideoThumbnailImpl videoThumbnailImpl;

  @BeforeClass
  public static void setUpClass() {
    Assume.assumeFalse(
        "Skip on Windows, fake FFmpeg scripts need a POSIX shell.", SystemUtils.IS_OS_WINDOWS);
  }

  @Before
  public void setUp() throws Exception {
    originalDdfHome = System.getProperty("ddf.home");
    ddfHome = temporaryFolder.newFolder("ddf-home");
    System.setProperty("ddf.home", ddfHome.getAbsolutePath());
    invocationLog = temporaryFolder.newFile("invocations.log");
    videoFile = temporaryFolder.newFile("video.mp4");

    final File bundledScript = temporaryFolder.newFile("bundled-ffmpeg");
    Files.write(
        bundledScript.toPath(),
        new FakeFfmpeg("bundled").script().getBytes(StandardCharsets.UTF_8));
    videoThumbnailImpl = new VideoThumbnailImpl(createMockBundleContext(bundledScript));
  }

  @After
  public void tearDown() {
    if (videoThumbnailImpl != null) {
      videoThumbnailImpl.destroy();
    }
    if (originalDdfHome == null) {
      System.clearProperty("ddf.home");
    } else {
      System.setProperty("ddf.home", originalDdfHome);
    }
  }

  @Test
  public void testConfiguredFfmpegPath() throws Exception {
    videoThumbnailImpl.setFfmpegPath(install(new FakeFfmpeg("configured")).getAbsolutePath());

    final Optional<byte[]> thumbnail = runThumbnail();

    assertThat(new String(thumbnail.get(), StandardCharsets.US_ASCII), is("gif"));
    assertThat(invokedBinaries(), everyItem(is("configured")));
  }

  @Test
  public void testNoDurationMakesStaticThumbnail() throws Exception {
    final FakeFfmpeg fake = new FakeFfmpeg("configured");
    fake.duration = null;
    videoThumbnailImpl.setFfmpegPath(install(fake).getAbsolutePath());

    final Optional<byte[]> thumbnail = runThumbnail();

    assertThat(new String(thumbnail.get(), StandardCharsets.US_ASCII), is("png"));
  }

  @Test
  public void testBlankFfmpegPathUsesBundled() throws Exception {
    videoThumbnailImpl.setFfmpegPath(install(new FakeFfmpeg("configured")).getAbsolutePath());
    videoThumbnailImpl.setFfmpegPath("  ");

    final Optional<byte[]> thumbnail = runThumbnail();

    assertThat(thumbnail.isPresent(), is(true));
    assertThat(invokedBinaries(), everyItem(is("bundled")));
  }

  @Test
  public void testNullFfmpegPathUsesBundled() throws Exception {
    videoThumbnailImpl.setFfmpegPath(install(new FakeFfmpeg("configured")).getAbsolutePath());
    videoThumbnailImpl.setFfmpegPath(null);

    final Optional<byte[]> thumbnail = runThumbnail();

    assertThat(thumbnail.isPresent(), is(true));
    assertThat(invokedBinaries(), everyItem(is("bundled")));
  }

  @Test
  public void testDestroyKeepsConfiguredBinary() throws Exception {
    final File configured = install(new FakeFfmpeg("configured"));
    final File bundledDir = new File(ddfHome, "bin_third_party/ffmpeg");
    videoThumbnailImpl.setFfmpegPath(configured.getAbsolutePath());

    videoThumbnailImpl.destroy();

    assertThat(configured.exists(), is(true));
    assertThat(bundledDir.exists(), is(false));
  }

  @Test(expected = IOException.class)
  public void testFfmpegFails() throws Exception {
    final FakeFfmpeg fake = new FakeFfmpeg("configured");
    fake.thumbnailExitCode = 1;
    videoThumbnailImpl.setFfmpegPath(install(fake).getAbsolutePath());

    runThumbnail();
  }

  @Test
  public void testFfmpegWritesNoThumbnail() throws Exception {
    final FakeFfmpeg fake = new FakeFfmpeg("configured");
    fake.writesThumbnail = false;
    final String ffmpegPath = install(fake).getAbsolutePath();
    videoThumbnailImpl.setFfmpegPath(ffmpegPath);

    try {
      runThumbnail();
      fail("Expected an IOException when FFmpeg doesn't write a thumbnail");
    } catch (IOException e) {
      assertThat(e.getMessage(), containsString(ffmpegPath));
    }
  }

  private Optional<byte[]> runThumbnail() throws Exception {
    return videoThumbnailImpl.videoThumbnail(videoFile, new MimeType("video/mp4"));
  }

  private List<String> invokedBinaries() throws IOException {
    return Files.readAllLines(invocationLog.toPath()).stream()
        .map(line -> line.split(" ", 2)[0])
        .collect(Collectors.toList());
  }

  private File install(FakeFfmpeg fake) throws IOException {
    return install(fake, new File(temporaryFolder.newFolder(), "ffmpeg"));
  }

  private File install(FakeFfmpeg fake, File file) throws IOException {
    Files.createDirectories(file.getParentFile().toPath());
    Files.write(file.toPath(), fake.script().getBytes(StandardCharsets.UTF_8));
    if (!file.setExecutable(true)) {
      fail("Couldn't make " + file + " executable");
    }
    return file;
  }

  private BundleContext createMockBundleContext(File bundledScript) throws IOException {
    final BundleContext bundleContext = mock(BundleContext.class);
    final Bundle bundle = mock(Bundle.class);
    doReturn(bundle).when(bundleContext).getBundle();
    doReturn(bundledScript.toURI().toURL()).when(bundle).getEntry(anyString());
    return bundleContext;
  }

  private class FakeFfmpeg {
    private final String name;

    private String duration = "00:01:00.00";

    private int thumbnailExitCode = 0;

    private boolean writesThumbnail = true;

    private FakeFfmpeg(String name) {
      this.name = name;
    }

    private String script() {
      final String logInvocation =
          String.format("echo \"%s $*\" >> '%s'", name, invocationLog.getAbsolutePath());
      final String findOutputPathBeforeOverwriteFlag =
          "outputPath=''; last=''; for a in \"$@\"; do outputPath=\"$last\"; last=\"$a\"; done";
      final String reportDurationForInfoCommand =
          duration == null ? "exit 1" : "echo 'Duration: " + duration + "' >&2; exit 1";
      final String writeStubThumbnail = writesThumbnail ? "printf \"$stub\" > \"$outputPath\"" : "";
      return String.join(
          "\n",
          "#!/bin/sh",
          logInvocation,
          findOutputPathBeforeOverwriteFlag,
          "case \"$outputPath\" in",
          "  *.gif) stub='gif' ;;",
          "  *.png) stub='png' ;;",
          "  *) " + reportDurationForInfoCommand + " ;;",
          "esac",
          writeStubThumbnail,
          "exit " + thumbnailExitCode,
          "");
    }
  }
}
