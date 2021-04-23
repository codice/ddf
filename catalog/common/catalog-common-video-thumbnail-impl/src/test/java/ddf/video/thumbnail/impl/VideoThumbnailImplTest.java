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

import static ddf.video.thumbnail.impl.VideoThumbnailImpl.DEFAULT_MAX_FILE_SIZE_MB;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class VideoThumbnailImplTest {

  private static final MimeType VIDEO_MP4;

  private static final MimeType IMAGE_JPEG;

  private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private String binaryPath;

  private VideoThumbnailImpl videoThumbnailImpl;

  static {
    try {
      VIDEO_MP4 = new MimeType("video/mp4");
      IMAGE_JPEG = new MimeType("image/jpeg");
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @BeforeClass
  public static void setUpClass() {
    Assume.assumeFalse("Skip unit tests on Windows. See DDF-3503.", SystemUtils.IS_OS_WINDOWS);
  }

  @Before
  public void setUp() throws IOException {
    System.setProperty("ddf.home", SystemUtils.USER_DIR);

    binaryPath = FilenameUtils.concat(System.getProperty("ddf.home"), "bin_third_party");

    videoThumbnailImpl = new VideoThumbnailImpl(Objects.requireNonNull(createMockBundleContext()));
  }

  @After
  public void tearDown() {
    videoThumbnailImpl.destroy();

    final File binaryFolder = new File(binaryPath);
    if (binaryFolder.exists() && !FileUtils.deleteQuietly(binaryFolder)) {
      binaryFolder.deleteOnExit();
    }
  }

  /**
   * Tests processing short.mp4. This file is short enough that FFmpeg will only generate one
   * thumbnail for it even if we request more than one.
   */
  @Test
  public void testProcessShortVideo() throws Exception {
    // given
    final File file = getResource("/short.mp4");

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, VIDEO_MP4);

    assertThat(optionalBytes.isPresent(), is(true));
    verifyThumbnailIsPng(optionalBytes.get());
  }

  /**
   * Tests processing medium.mp4. This file is short enough that the plugin won't try to grab
   * thumbnails from different portions of the video but is long enough that the resulting thumbnail
   * will be a GIF.
   */
  @Test
  public void testProcessMediumVideo() throws Exception {
    // given
    final File file = getResource("/medium.mp4");

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, VIDEO_MP4);

    // then
    assertThat(optionalBytes.isPresent(), is(true));
    verifyThumbnailIsGif(optionalBytes.get());
  }

  /**
   * Tests processing long.mp4. This file is long enough that the plugin will try to grab thumbnails
   * from different portions of the video.
   */
  @Test
  public void testProcessLongVideo() throws Exception {
    // given
    final File file = getResource("/long.mp4");

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, VIDEO_MP4);

    // then
    assertThat(optionalBytes.isPresent(), is(true));
    verifyThumbnailIsGif(optionalBytes.get());
  }

  @Test
  public void testProcessNotVideoFile() throws Exception {
    // given
    File file = mock(File.class);

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, IMAGE_JPEG);

    // then
    assertThat(optionalBytes.isPresent(), is(false));
  }

  @Test(expected = IOException.class)
  public void testProcessCorruptedVideo() throws Exception {
    // given
    final File file = getResource("/corrupted.mp4");

    // when
    videoThumbnailImpl.videoThumbnail(file, VIDEO_MP4);
  }

  @Test
  public void testProcessVideoLargerThanDefaultMaxFileSize() throws Exception {
    // given
    File file = mock(File.class);
    when(file.length()).thenReturn(DEFAULT_MAX_FILE_SIZE_MB * BYTES_PER_MEGABYTE + 1);

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, IMAGE_JPEG);

    // then
    assertThat(optionalBytes.isPresent(), is(false));
  }

  @Test
  public void testProcessVideoLargerThanConfiguredMaxFileSize() throws Exception {
    // given
    final int maxFileSizeMB = 5;
    videoThumbnailImpl.setMaxFileSizeMB(maxFileSizeMB);
    File file = mock(File.class);
    when(file.length()).thenReturn((long) maxFileSizeMB);

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, IMAGE_JPEG);

    // then
    assertThat(optionalBytes.isPresent(), is(false));
  }

  @Test
  public void testProcessVideoWhenMaxFileSizeIs0() throws Exception {
    // given
    final File file = getResource("/short.mp4");
    videoThumbnailImpl.setMaxFileSizeMB(0);

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, VIDEO_MP4);

    // then
    assertThat(optionalBytes.isPresent(), is(false));
  }

  @Test
  public void testProcessVideoSmallerThanConfiguredMaxFileSize() throws Exception {
    // given
    final File file = getResource("/long.mp4");
    videoThumbnailImpl.setMaxFileSizeMB(1);

    // when
    Optional<byte[]> optionalBytes = videoThumbnailImpl.videoThumbnail(file, VIDEO_MP4);

    // then
    assertThat(optionalBytes.isPresent(), is(true));
    verifyThumbnailIsGif(optionalBytes.get());
  }

  /** create mock methods */
  private BundleContext createMockBundleContext() {
    final BundleContext mockBundleContext = mock(BundleContext.class);

    final Bundle mockBundle = mock(Bundle.class);
    doReturn(mockBundle).when(mockBundleContext).getBundle();

    String ffmpegResourcePath;
    URL ffmpegBinaryUrl;

    if (SystemUtils.IS_OS_LINUX) {
      ffmpegResourcePath = "linux/ffmpeg-4.3.1";
    } else if (SystemUtils.IS_OS_MAC) {
      ffmpegResourcePath = "osx/ffmpeg-4.3.1";
      //      Skip unit tests on Windows. See DDF-3503.
      //    } else if (SystemUtils.IS_OS_WINDOWS) {
      //      ffmpegResourcePath = "windows/ffmpeg-4.3.1.exe";
    } else {
      fail(
          "Platform is not Linux, Mac, or Windows. No FFmpeg binaries are provided for this platform.");
      return null;
    }

    ffmpegBinaryUrl = getClass().getClassLoader().getResource(ffmpegResourcePath);

    doReturn(ffmpegBinaryUrl).when(mockBundle).getEntry(ffmpegResourcePath);

    return mockBundleContext;
  }

  private void verifyThumbnailIsGif(byte[] thumbnail) {
    assertThat("The thumbnail should not be null", thumbnail, notNullValue());

    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[0], is((byte) 0x47));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[1], is((byte) 0x49));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[2], is((byte) 0x46));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[3], is((byte) 0x38));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[4], is((byte) 0x39));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[5], is((byte) 0x61));
  }

  private void verifyThumbnailIsPng(byte[] thumbnail) {
    assertThat("The thumbnail should not be null", thumbnail, notNullValue());

    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[0], is((byte) 0x89));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[1], is((byte) 0x50));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[2], is((byte) 0x4E));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[3], is((byte) 0x47));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[4], is((byte) 0x0D));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[5], is((byte) 0x0A));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[6], is((byte) 0x1A));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[7], is((byte) 0x0A));
  }

  private File getResource(String resource) throws URISyntaxException {
    return Paths.get(Objects.requireNonNull(getClass().getResource(resource)).toURI()).toFile();
  }
}
