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

  private static final Duration MAX_THUMBNAIL_SEARCH_WINDOW = Duration.ofSeconds(4);

  private static final String SUPPRESS_PRINTING_BANNER_FLAG = "-hide_banner";

  private static final String INPUT_FILE_FLAG = "-i";

  private static final boolean DONT_HANDLE_QUOTING = false;

  private static final String OVERWRITE_EXISTING_FILE_FLAG = "-y";

  private static final PumpStreamHandler DEV_NULL = new PumpStreamHandler(NULL_OUTPUT_STREAM);

  private static final int TIMEOUT_SECONDS = 15;

  private final String bundledFfmpegPath;

  private volatile String configuredFfmpegPath;

  private int maxFileSizeMB = DEFAULT_MAX_FILE_SIZE_MB;

  private final Semaphore limitFFmpegProcessesSemaphore;

  public VideoThumbnailImpl(final BundleContext bundleContext) throws IOException {
    final String bundledFFmpegBinaryPath = getBundledFFmpegBinaryPath();
    final String ffmpegBinaryName = StringUtils.substringAfterLast(bundledFFmpegBinaryPath, "/");
    final String ffmpegFolderPath =
        FilenameUtils.concat(System.getProperty("ddf.home"), "bin_third_party/ffmpeg");
    bundledFfmpegPath = FilenameUtils.concat(ffmpegFolderPath, ffmpegBinaryName);

    try (final InputStream inputStream =
        bundleContext.getBundle().getEntry(bundledFFmpegBinaryPath).openStream()) {
      copyFFmpegBinary(inputStream);
    }

    limitFFmpegProcessesSemaphore = new Semaphore(MAX_FFMPEG_PROCESSES, true);
  }

  public void setMaxFileSizeMB(int maxFileSizeMB) {
    this.maxFileSizeMB = maxFileSizeMB;
  }

  public void setFfmpegPath(String ffmpegPath) {
    this.configuredFfmpegPath = StringUtils.isBlank(ffmpegPath) ? null : ffmpegPath;
  }

  private String getFfmpegPath() {
    return configuredFfmpegPath != null ? configuredFfmpegPath : bundledFfmpegPath;
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

    limitFFmpegProcessesSemaphore.acquire();

    try {
      final byte[] thumbnailBytes = createThumbnail(file.toString());
      LOGGER.debug("Successfully created video thumbnail for {} with mime-type {}", file, mimeType);
      return Optional.of(thumbnailBytes);
    } finally {
      limitFFmpegProcessesSemaphore.release();
    }
  }

  public boolean isTooLarge(long size) {
    final long maxFileSizeBytes = maxFileSizeMB * MEGABYTE;
    return size > maxFileSizeBytes;
  }

  private byte[] createThumbnail(final String videoFilePath)
      throws IOException, InterruptedException {
    Duration videoDuration = null;

    try {
      videoDuration = getVideoDuration(videoFilePath);
    } catch (InterruptedException e) {
      LOGGER.debug(
          "InterruptedException occured while getting video duration from FFmpeg output for videoFilePath={}",
          videoFilePath,
          e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOGGER.debug(
          "Couldn't get video duration from FFmpeg output for videoFilePath={}.", videoFilePath, e);
    }

    final boolean hasDuration = videoDuration != null && !videoDuration.isZero();
    final File thumbnailFile = File.createTempFile("thumbnail-", hasDuration ? ".gif" : ".png");

    try {
      final CommandLine command =
          hasDuration
              ? getFFmpegCreateAnimatedGifCommand(videoFilePath, thumbnailFile, videoDuration)
              : getFFmpegCreateThumbnailCommand(videoFilePath, thumbnailFile);
      final DefaultExecuteResultHandler resultHandler = executeFFmpeg(command, DEV_NULL);

      resultHandler.waitFor();

      if (resultHandler.getException() != null) {
        throw resultHandler.getException();
      }

      if (thumbnailFile.length() == 0) {
        throw new IOException(
            "FFmpeg binary " + command.getExecutable() + " didn't produce a thumbnail.");
      }

      return FileUtils.readFileToByteArray(thumbnailFile);
    } finally {
      if (!thumbnailFile.delete()) {
        thumbnailFile.deleteOnExit();
      }
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

  private CommandLine getFFmpegCreateAnimatedGifCommand(
      final String videoFilePath, final File gifFile, final Duration duration) {
    final String seekFlag = "-ss";
    final String searchWindowFlag = "-t";
    final String filterGraphFlag = "-filter_complex";
    final String mapFlag = "-map";
    final String outputLabel = "[o]";
    final String loopFlag = "-loop";
    final String loopValue = "0";
    final String lastFrameDelayFlag = "-final_delay";
    final String oneSecondInCentiseconds = "100";

    final Duration durationFraction = duration.dividedBy(THUMBNAIL_COUNT);
    final Duration searchWindow =
        durationFraction.compareTo(MAX_THUMBNAIL_SEARCH_WINDOW) < 0
            ? durationFraction
            : MAX_THUMBNAIL_SEARCH_WINDOW;

    final CommandLine command =
        new CommandLine(getFfmpegPath()).addArgument(SUPPRESS_PRINTING_BANNER_FLAG);

    for (int i = 0; i < THUMBNAIL_COUNT; ++i) {
      command
          .addArgument(seekFlag)
          .addArgument(durationToString(durationFraction.multipliedBy(i)))
          .addArgument(searchWindowFlag)
          .addArgument(durationToString(searchWindow))
          .addArgument(INPUT_FILE_FLAG)
          .addArgument(videoFilePath, DONT_HANDLE_QUOTING);
    }

    command
        .addArgument(filterGraphFlag)
        .addArgument(getThumbnailFilterGraph(outputLabel), DONT_HANDLE_QUOTING)
        .addArgument(mapFlag)
        .addArgument(outputLabel)
        .addArgument(loopFlag)
        .addArgument(loopValue)
        .addArgument(lastFrameDelayFlag)
        .addArgument(oneSecondInCentiseconds)
        .addArgument(gifFile.getPath(), DONT_HANDLE_QUOTING)
        .addArgument(OVERWRITE_EXISTING_FILE_FLAG);

    return command;
  }

  private String getThumbnailFilterGraph(final String outputLabel) {
    final StringBuilder filterGraph = new StringBuilder();
    final StringBuilder concatInputs = new StringBuilder();

    for (int i = 0; i < THUMBNAIL_COUNT; ++i) {
      filterGraph.append(
          String.format("[%d:v]thumbnail,trim=end_frame=1,scale=200:-2[v%d];", i, i));
      concatInputs.append(String.format("[v%d]", i));
    }

    return filterGraph
        .append(concatInputs)
        .append(String.format("concat=n=%d,setpts=N/TB%s", THUMBNAIL_COUNT, outputLabel))
        .toString();
  }

  private CommandLine getFFmpegCreateThumbnailCommand(
      final String videoFilePath, final File thumbnailFile) {
    final String filterChainFlag = "-vf";
    final String filterChain = "thumbnail,scale=200:-1";
    final String videoFramesToOutputFlag = "-frames:v";
    final String videoFramesToOutput = "1";

    return new CommandLine(getFfmpegPath())
        .addArgument(SUPPRESS_PRINTING_BANNER_FLAG)
        .addArgument(INPUT_FILE_FLAG)
        .addArgument(videoFilePath, DONT_HANDLE_QUOTING)
        .addArgument(filterChainFlag)
        .addArgument(filterChain)
        .addArgument(videoFramesToOutputFlag)
        .addArgument(videoFramesToOutput)
        .addArgument(thumbnailFile.getPath(), DONT_HANDLE_QUOTING)
        .addArgument(OVERWRITE_EXISTING_FILE_FLAG);
  }

  private String durationToString(final Duration duration) {
    final long seconds = duration.getSeconds();
    return String.format(
        "%02d:%02d:%02d.%03d",
        seconds / 3600, (seconds % 3600) / 60, seconds % 60, duration.toMillisPart());
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
        new CommandLine(getFfmpegPath())
            .addArgument(SUPPRESS_PRINTING_BANNER_FLAG)
            .addArgument(INPUT_FILE_FLAG)
            .addArgument(videoFilePath, DONT_HANDLE_QUOTING);
    LOGGER.trace("FFmpeg command : {}", commandLine);
    return commandLine;
  }

  /**
   * Deletes the directory that holds the bundled FFmpeg binary.
   *
   * <p>Called by Blueprint.
   */
  public void destroy() {
    if (bundledFfmpegPath != null) {
      String fullPathNoEndSeparator = FilenameUtils.getFullPathNoEndSeparator(bundledFfmpegPath);
      if (fullPathNoEndSeparator == null) {
        fullPathNoEndSeparator = bundledFfmpegPath;
      }
      final File ffmpegDirectory = new File(fullPathNoEndSeparator);
      if (!FileUtils.deleteQuietly(ffmpegDirectory)) {
        ffmpegDirectory.deleteOnExit();
      }
    }
  }

  private void copyFFmpegBinary(final InputStream inputStream) throws IOException {
    final File ffmpegBinary = new File(bundledFfmpegPath);

    if (!ffmpegBinary.exists()) {
      FileUtils.copyInputStreamToFile(inputStream, ffmpegBinary);
      if (!ffmpegBinary.setExecutable(true)) {
        LOGGER.warn(
            "Couldn't make FFmpeg binary at {} executable. It must be executable by its owner for the plugin to work.",
            bundledFfmpegPath);
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
