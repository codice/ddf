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

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

import com.google.common.net.MediaType;
import ddf.video.thumbnail.VideoThumbnail;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.MimeType;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoThumbnailImpl implements VideoThumbnail {

  private static final Logger LOGGER = LoggerFactory.getLogger(VideoThumbnailImpl.class);

  private static final long MEGABYTE = 1024L * 1024L;

  private static final int MAX_FFMPEG_PROCESSES = 4;

  public static final int DEFAULT_MAX_FILE_SIZE_MB = 120;

  private static final int THUMBNAIL_COUNT = 3;

  private static final int FFMPEG_FILE_NUMBERING_START = 1;

  private static final int ROUGH_MINIMUM_SECONDS_FOR_MULTIPLE_THUMBNAILS = 10;

  private static final String SUPPRESS_PRINTING_BANNER_FLAG = "-hide_banner";

  private static final String INPUT_FILE_FLAG = "-i";

  private static final boolean DONT_HANDLE_QUOTING = false;

  private static final String OVERWRITE_EXISTING_FILE_FLAG = "-y";

  private static final PumpStreamHandler DEV_NULL = new PumpStreamHandler(NULL_OUTPUT_STREAM);

  private static final int TIMEOUT_SECONDS = 15;

  private final String ffmpegPath;

  private int maxFileSizeMB = DEFAULT_MAX_FILE_SIZE_MB;

  private final Semaphore limitFFmpegProcessesSemaphore;

  public VideoThumbnailImpl(final BundleContext bundleContext) throws IOException {
    final String bundledFFmpegBinaryPath = getBundledFFmpegBinaryPath();
    final String ffmpegBinaryName = StringUtils.substringAfterLast(bundledFFmpegBinaryPath, "/");
    final String ffmpegFolderPath =
        FilenameUtils.concat(System.getProperty("ddf.home"), "bin_third_party/ffmpeg");
    ffmpegPath = FilenameUtils.concat(ffmpegFolderPath, ffmpegBinaryName);

    try (final InputStream inputStream =
        bundleContext.getBundle().getEntry(bundledFFmpegBinaryPath).openStream()) {
      copyFFmpegBinary(inputStream);
    }

    limitFFmpegProcessesSemaphore = new Semaphore(MAX_FFMPEG_PROCESSES, true);
  }

  public void setMaxFileSizeMB(int maxFileSizeMB) {
    this.maxFileSizeMB = maxFileSizeMB;
  }

  @SuppressWarnings("UnstableApiUsage" /* we already using these APIs in the video plugin */)
  @Override
  public boolean isVideo(MimeType mimeType) {
    final MediaType createdMediaType =
        MediaType.create(mimeType.getPrimaryType(), mimeType.getSubType());
    return createdMediaType.is(MediaType.ANY_VIDEO_TYPE);
  }

  @Override
  public Optional<byte[]> videoThumbnail(File file, MimeType mimeType)
      throws IOException, InterruptedException {

    if (!isVideo(mimeType)) {
      return Optional.empty();
    }

    if (isTooLarge(file.length())) {
      LOGGER.debug(
          "Content (file={} size={} MB) is larger than the configured max file size to process ({} MB).",
          file,
          file.length() / MEGABYTE,
          maxFileSizeMB);
      return Optional.empty();
    }

    LOGGER.trace("About to create video thumbnail");

    try {
      limitFFmpegProcessesSemaphore.acquire();

      try {
        final byte[] thumbnailBytes = createThumbnail(file.toString());
        LOGGER.debug(
            "Successfully created video thumbnail for {} with mime-type {}", file, mimeType);
        return Optional.of(thumbnailBytes);
      } finally {
        limitFFmpegProcessesSemaphore.release();
      }
    } finally {
      deleteImageFiles();
    }
  }

  public boolean isTooLarge(long size) {
    final long maxFileSizeBytes = maxFileSizeMB * MEGABYTE;
    return size > maxFileSizeBytes;
  }

  private void deleteImageFiles() {
    final List<File> imageFiles = getThumbnailFiles();

    imageFiles.add(new File(getGifFilePath()));

    imageFiles.forEach(
        file -> {
          if (file.exists() && !file.delete()) {
            file.deleteOnExit();
          }
        });
  }

  private String getGifFilePath() {
    final String thumbnailFilePath = getThumbnailFilePath();
    return thumbnailFilePath.substring(0, thumbnailFilePath.lastIndexOf('-')) + ".gif";
  }

  private String getThumbnailFilePath() {
    final long threadId = Thread.currentThread().getId();

    // FFmpeg replaces the "%1d" with a single digit when it creates the output file. This is
    // necessary because FFmpeg requires a unique filename for each output file when outputting
    // multiple images.
    final String thumbnailFileName = String.format("thumbnail-%d-%%1d.png", threadId);

    final String tempDirectoryPath = System.getProperty("java.io.tmpdir");

    return FilenameUtils.concat(tempDirectoryPath, thumbnailFileName);
  }

  private List<File> getThumbnailFiles() {
    final List<File> thumbnailFiles = new ArrayList<>(THUMBNAIL_COUNT);

    final String thumbnailFilePath = getThumbnailFilePath();

    // FFmpeg starts numbering files with 1.
    for (int i = FFMPEG_FILE_NUMBERING_START; i <= THUMBNAIL_COUNT; ++i) {
      final File thumbnailFile = new File(String.format(thumbnailFilePath, i));
      if (thumbnailFile.exists()) {
        thumbnailFiles.add(new File(String.format(thumbnailFilePath, i)));
      }
    }

    return thumbnailFiles;
  }

  private byte[] createThumbnail(final String videoFilePath)
      throws IOException, InterruptedException {
    Duration videoDuration = null;

    try {
      videoDuration = getVideoDuration(videoFilePath);
    } catch (Exception e) {
      LOGGER.debug(
          "Couldn't get video duration from FFmpeg output for videoFilePath={}.", videoFilePath, e);
    }

    /* Realistically, to get good thumbnails by dividing a video into segments, the video
    should be at least 10 seconds long. This is because FFmpeg looks for thumbnails in
    batches of 100 frames each, and these frames usually come from the portion of the
    video immediately following the seek position. If the video isn't long enough,
    the regions of the video FFmpeg will pick thumbnails from will likely overlap,
    causing the same thumbnail to be generated for multiple segments. */
    if (videoDuration != null
        && videoDuration.getSeconds() > ROUGH_MINIMUM_SECONDS_FOR_MULTIPLE_THUMBNAILS) {
      return createGifThumbnailWithDuration(videoFilePath, videoDuration);
    } else {
      return createThumbnailWithoutDuration(videoFilePath);
    }
  }

  private byte[] createThumbnailWithoutDuration(final String videoFilePath)
      throws IOException, InterruptedException {
    generateThumbnailsWithoutDuration(videoFilePath);

    final List<File> thumbnailFiles = getThumbnailFiles();

    // FFmpeg looks for thumbnails in batches of 100 frames each, so even if we request more
    // than one thumbnail for a very short video, we will only get one back.
    if (thumbnailFiles.size() == 1) {
      return createStaticImageThumbnail();
    } else {
      return createGifFromThumbnailFiles();
    }
  }

  private byte[] createGifFromThumbnailFiles() throws IOException, InterruptedException {
    final DefaultExecuteResultHandler resultHandler =
        executeFFmpeg(getFFmpegCreateAnimatedGifCommand(), DEV_NULL);

    resultHandler.waitFor();

    if (resultHandler.getException() == null) {
      return FileUtils.readFileToByteArray(new File(getGifFilePath()));
    } else {
      throw resultHandler.getException();
    }
  }

  private DefaultExecuteResultHandler executeFFmpeg(
      final CommandLine command, final PumpStreamHandler streamHandler) throws IOException {
    final ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT_SECONDS * 1000L);
    final Executor executor = new DefaultExecutor();
    final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

    if (streamHandler != null) {
      executor.setStreamHandler(streamHandler);
    }
    executor.setWatchdog(watchdog);
    executeWithPrivilege(command, executor, resultHandler);
    return resultHandler;
  }

  private void executeWithPrivilege(
      CommandLine command, Executor executor, DefaultExecuteResultHandler resultHandler)
      throws IOException {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                executor.execute(command, resultHandler);
                return null;
              });
    } catch (PrivilegedActionException e) {
      String msg = "Video thumbnail plugin failed to execute ffmepg";
      LOGGER.info(msg);
      LOGGER.debug(msg, e);
      Throwable cause = e.getCause();
      // org.apache.commons.exe.Executor's execute() method's signature includes a throws clause
      // for ExecuteException and IOException. ExecuteException is a subclass of IOException.
      if (cause instanceof IOException) {
        throw (IOException) cause;
      }
    }
  }

  private CommandLine getFFmpegCreateAnimatedGifCommand() {
    final String framerateFlag = "-framerate";
    final String framerate = "1";
    final String loopFlag = "-loop";
    final String loopValue = "0";

    return new CommandLine(ffmpegPath)
        .addArgument(SUPPRESS_PRINTING_BANNER_FLAG)
        .addArgument(framerateFlag)
        .addArgument(framerate)
        .addArgument(INPUT_FILE_FLAG)
        .addArgument(getThumbnailFilePath(), DONT_HANDLE_QUOTING)
        .addArgument(loopFlag)
        .addArgument(loopValue)
        .addArgument(getGifFilePath(), DONT_HANDLE_QUOTING)
        .addArgument(OVERWRITE_EXISTING_FILE_FLAG);
  }

  private byte[] createStaticImageThumbnail() throws IOException {
    return FileUtils.readFileToByteArray(getThumbnailFiles().get(0));
  }

  private void generateThumbnailsWithoutDuration(final String videoFilePath)
      throws IOException, InterruptedException {
    final CommandLine command =
        getFFmpegCreateThumbnailCommand(
            videoFilePath, getThumbnailFilePath(), null, THUMBNAIL_COUNT);
    final DefaultExecuteResultHandler resultHandler = executeFFmpeg(command, DEV_NULL);

    resultHandler.waitFor();

    if (resultHandler.getException() != null) {
      throw resultHandler.getException();
    }
  }

  private CommandLine getFFmpegCreateThumbnailCommand(
      final String videoFilePath,
      final String thumbnailFilePath,
      final String seek,
      final int numFrames) {
    final String filterChainFlag = "-vf";
    final String filterChain = "thumbnail,scale=200:-1";
    final String videoFramesToOutputFlag = "-frames:v";
    final String videoFramesToOutput = String.valueOf(numFrames);
    final String videoSyncFlag = "-vsync";
    final String videoSyncVariableFrameRate = "vfr";

    final CommandLine command =
        new CommandLine(ffmpegPath).addArgument(SUPPRESS_PRINTING_BANNER_FLAG);

    if (seek != null) {
      final String seekFlag = "-ss";
      command.addArgument(seekFlag).addArgument(seek);
    }

    command
        .addArgument(INPUT_FILE_FLAG)
        .addArgument(videoFilePath, DONT_HANDLE_QUOTING)
        .addArgument(filterChainFlag)
        .addArgument(filterChain)
        .addArgument(videoFramesToOutputFlag)
        .addArgument(videoFramesToOutput)
        // The "-vsync vfr" argument prevents frames from being duplicated, which allows us
        // to get a different thumbnail for each of the output images.
        .addArgument(videoSyncFlag)
        .addArgument(videoSyncVariableFrameRate);

    command
        .addArgument(thumbnailFilePath, DONT_HANDLE_QUOTING)
        .addArgument(OVERWRITE_EXISTING_FILE_FLAG);

    return command;
  }

  private byte[] createGifThumbnailWithDuration(final String videoFilePath, final Duration duration)
      throws IOException, InterruptedException {
    final Duration durationFraction = duration.dividedBy(THUMBNAIL_COUNT);

    // Start numbering files with 1 to match FFmpeg's convention.
    for (int clipNum = FFMPEG_FILE_NUMBERING_START; clipNum <= THUMBNAIL_COUNT; ++clipNum) {
      final String thumbnailPath = String.format(getThumbnailFilePath(), clipNum);

      final String seek = durationToString(durationFraction.multipliedBy((long) clipNum - 1));

      final CommandLine command =
          getFFmpegCreateThumbnailCommand(videoFilePath, thumbnailPath, seek, 1);

      final DefaultExecuteResultHandler resultHandler = executeFFmpeg(command, DEV_NULL);
      resultHandler.waitFor();
    }

    return createGifFromThumbnailFiles();
  }

  private String durationToString(final Duration duration) {
    final long seconds = duration.getSeconds();
    return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
  }

  private Duration getVideoDuration(final String videoFilePath)
      throws IOException, InterruptedException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    final CommandLine command = getFFmpegInfoCommand(videoFilePath);
    final DefaultExecuteResultHandler resultHandler = executeFFmpeg(command, streamHandler);
    resultHandler.waitFor();

    return parseVideoDuration(outputStream.toString(StandardCharsets.UTF_8.name()));
  }

  private Duration parseVideoDuration(final String ffmpegOutput) throws IOException {
    LOGGER.trace("FFmpeg output : {}", ffmpegOutput);
    final Pattern pattern = Pattern.compile("Duration: \\d\\d:\\d\\d:\\d\\d\\.\\d+");
    final Matcher matcher = pattern.matcher(ffmpegOutput);

    if (matcher.find()) {
      final String durationString = matcher.group();
      final String[] durationParts = durationString.substring("Duration: ".length()).split(":");
      final String hours = durationParts[0];
      final String minutes = durationParts[1];
      final String seconds = durationParts[2];
      return Duration.parse(String.format("PT%sH%sM%sS", hours, minutes, seconds));
    } else {
      throw new IOException("Video duration not found in FFmpeg output.");
    }
  }

  private CommandLine getFFmpegInfoCommand(final String videoFilePath) {
    CommandLine commandLine =
        new CommandLine(ffmpegPath)
            .addArgument(SUPPRESS_PRINTING_BANNER_FLAG)
            .addArgument(INPUT_FILE_FLAG)
            .addArgument(videoFilePath, DONT_HANDLE_QUOTING);
    LOGGER.trace("FFmpeg command : {}", commandLine);
    return commandLine;
  }

  /**
   * Deletes the directory that holds the FFmpeg binary.
   *
   * <p>Called by Blueprint.
   */
  public void destroy() {
    if (ffmpegPath != null) {
      String fullPathNoEndSeparator = FilenameUtils.getFullPathNoEndSeparator(ffmpegPath);
      if (fullPathNoEndSeparator == null) {
        fullPathNoEndSeparator = ffmpegPath;
      }
      final File ffmpegDirectory = new File(fullPathNoEndSeparator);
      if (!FileUtils.deleteQuietly(ffmpegDirectory)) {
        ffmpegDirectory.deleteOnExit();
      }
    }
  }

  private void copyFFmpegBinary(final InputStream inputStream) throws IOException {
    final File ffmpegBinary = new File(ffmpegPath);

    if (!ffmpegBinary.exists()) {
      FileUtils.copyInputStreamToFile(inputStream, ffmpegBinary);
      if (!ffmpegBinary.setExecutable(true)) {
        LOGGER.warn(
            "Couldn't make FFmpeg binary at {} executable. It must be executable by its owner for the plugin to work.",
            ffmpegPath);
      }
    }
  }

  private String getBundledFFmpegBinaryPath() {
    if (SystemUtils.IS_OS_LINUX) {
      return "linux/ffmpeg-4.3.1";
    } else if (SystemUtils.IS_OS_MAC) {
      return "osx/ffmpeg-4.3.1";
    } else if (SystemUtils.IS_OS_WINDOWS) {
      return "windows/ffmpeg-4.3.1.exe";
    } else {
      throw new IllegalStateException(
          "OS is not Linux, Mac, or Windows."
              + " No FFmpeg binary is available for this OS, so the plugin will not work.");
    }
  }
}
