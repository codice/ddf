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
package ddf.catalog.resource.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class DownloadInfoTest {

  private static final String DOWNLOAD_ID_KEY = "downloadId";

  private static final String FILE_NAME_KEY = "fileName";

  private static final String BYTES_DOWNLOADED_KEY = "bytesDownloaded";

  private static final String PERCENT_KEY = "percent";

  private static final String USER_KEY = "user";

  private static final String STATUS_KEY = "status";

  private static final String DOWNLOAD_ID = "03e3f850-240c-4cc6-a5a3-318dc34ad4bd";

  private static final String FILE_NAME = "image.jpg";

  private static final String BYTES_DOWNLOADED = "862978048";

  private static final String PERCENT = "95";

  private static final String ADMIN_USER = "admin";

  private Map<String, String> downloadStatusMap;

  @Before
  public void setup() {
    downloadStatusMap = new HashMap<>();
    downloadStatusMap.put(DOWNLOAD_ID_KEY, DOWNLOAD_ID);
    downloadStatusMap.put(FILE_NAME_KEY, FILE_NAME);
    downloadStatusMap.put(STATUS_KEY, DownloadState.IN_PROGRESS.name());
    downloadStatusMap.put(BYTES_DOWNLOADED_KEY, BYTES_DOWNLOADED);
    downloadStatusMap.put(PERCENT_KEY, PERCENT);
    downloadStatusMap.put(USER_KEY, ADMIN_USER);
  }

  @Test
  public void testDownloadInfoParse() {
    DownloadInfo downloadInfo = new DownloadInfo(downloadStatusMap);

    assertThat(downloadInfo.getDownloadId(), is(DOWNLOAD_ID));
    assertThat(downloadInfo.getFileName(), is(FILE_NAME));
    assertThat(downloadInfo.getStatus(), is(DownloadState.IN_PROGRESS.name()));
    assertThat(downloadInfo.getBytesDownloaded(), is(Long.parseLong(BYTES_DOWNLOADED)));
    assertThat(downloadInfo.getPercentDownloaded(), is(PERCENT));
    assertThat(downloadInfo.getUsers(), containsInAnyOrder(ADMIN_USER));
  }

  @Test(expected = NullPointerException.class)
  public void testDownloadInfoParseWithNullMap() {
    new DownloadInfo(null);
  }

  @Test(expected = NullPointerException.class)
  public void testDownloadInfoParseWithMissingDownloadId() {
    downloadStatusMap.remove(DOWNLOAD_ID_KEY);
    new DownloadInfo(downloadStatusMap);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDownloadInfoParseWithEmptyDownloadId() {
    downloadStatusMap.put(DOWNLOAD_ID_KEY, "");
    new DownloadInfo(downloadStatusMap);
  }

  @Test(expected = NullPointerException.class)
  public void testDownloadInfoParseWithMissingFileName() {
    downloadStatusMap.remove(FILE_NAME_KEY);
    new DownloadInfo(downloadStatusMap);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDownloadInfoParseWithEmptyFileName() {
    downloadStatusMap.put(FILE_NAME_KEY, "");
    new DownloadInfo(downloadStatusMap);
  }

  @Test(expected = NullPointerException.class)
  public void testDownloadInfoParseWithMissingStatus() {
    downloadStatusMap.remove(STATUS_KEY);
    new DownloadInfo(downloadStatusMap);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDownloadInfoParseWithEmptyStatus() {
    downloadStatusMap.put(STATUS_KEY, "");
    new DownloadInfo(downloadStatusMap);
  }

  @Test
  public void testDownloadInfoParseWithMissingBytesDownloaded() {
    downloadStatusMap.remove(BYTES_DOWNLOADED_KEY);

    DownloadInfo downloadInfo = new DownloadInfo(downloadStatusMap);

    assertThat(downloadInfo.getDownloadId(), is(DOWNLOAD_ID));
    assertThat(downloadInfo.getFileName(), is(FILE_NAME));
    assertThat(downloadInfo.getStatus(), is(DownloadState.IN_PROGRESS.name()));
    assertThat(downloadInfo.getBytesDownloaded(), is(0L));
    assertThat(downloadInfo.getPercentDownloaded(), is(PERCENT));
    assertThat(downloadInfo.getUsers(), containsInAnyOrder(ADMIN_USER));
  }

  @Test
  public void testDownloadInfoParseWithMissingPercent() {
    downloadStatusMap.remove(PERCENT_KEY);

    DownloadInfo downloadInfo = new DownloadInfo(downloadStatusMap);

    assertThat(downloadInfo.getDownloadId(), is(DOWNLOAD_ID));
    assertThat(downloadInfo.getFileName(), is(FILE_NAME));
    assertThat(downloadInfo.getStatus(), is(DownloadState.IN_PROGRESS.name()));
    assertThat(downloadInfo.getBytesDownloaded(), is(Long.parseLong(BYTES_DOWNLOADED)));
    assertThat(downloadInfo.getPercentDownloaded(), is("0"));
    assertThat(downloadInfo.getUsers(), containsInAnyOrder(ADMIN_USER));
  }

  @Test
  public void testDownloadInfoParseWithMissingUser() {
    downloadStatusMap.remove(USER_KEY);

    DownloadInfo downloadInfo = new DownloadInfo(downloadStatusMap);

    assertThat(downloadInfo.getDownloadId(), is(DOWNLOAD_ID));
    assertThat(downloadInfo.getFileName(), is(FILE_NAME));
    assertThat(downloadInfo.getStatus(), is(DownloadState.IN_PROGRESS.name()));
    assertThat(downloadInfo.getBytesDownloaded(), is(Long.parseLong(BYTES_DOWNLOADED)));
    assertThat(downloadInfo.getPercentDownloaded(), is(PERCENT));
    assertThat(downloadInfo.getUsers(), is(empty()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDownloadInfoParseWithBytesDownloadedUnparseable() {
    downloadStatusMap.put(BYTES_DOWNLOADED_KEY, "abc");

    new DownloadInfo(downloadStatusMap);
  }
}
