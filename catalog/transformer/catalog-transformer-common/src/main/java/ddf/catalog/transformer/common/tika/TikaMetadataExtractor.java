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
package ddf.catalog.transformer.common.tika;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.transformer.common.tika.handler.BodyAndMetadataContentHandler;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.fork.ForkParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.DelegatingParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ErrorParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.apple.AppleSingleFileParser;
import org.apache.tika.parser.apple.PListParser;
import org.apache.tika.parser.asm.ClassParser;
import org.apache.tika.parser.audio.AudioParser;
import org.apache.tika.parser.audio.MidiParser;
import org.apache.tika.parser.code.SourceCodeParser;
import org.apache.tika.parser.crypto.Pkcs7Parser;
import org.apache.tika.parser.crypto.TSDParser;
import org.apache.tika.parser.csv.TextAndCSVParser;
import org.apache.tika.parser.dbf.DBFParser;
import org.apache.tika.parser.dgn.DGN8Parser;
import org.apache.tika.parser.dif.DIFParser;
import org.apache.tika.parser.dwg.DWGParser;
import org.apache.tika.parser.dwg.DWGReadParser;
import org.apache.tika.parser.epub.EpubContentParser;
import org.apache.tika.parser.epub.EpubParser;
import org.apache.tika.parser.epub.OPFParser;
import org.apache.tika.parser.executable.ExecutableParser;
import org.apache.tika.parser.executable.UniversalExecutableParser;
import org.apache.tika.parser.external.CompositeExternalParser;
import org.apache.tika.parser.external2.ExternalParser;
import org.apache.tika.parser.feed.FeedParser;
import org.apache.tika.parser.font.AdobeFontMetricParser;
import org.apache.tika.parser.font.TrueTypeParser;
import org.apache.tika.parser.html.JSoupParser;
import org.apache.tika.parser.http.HttpParser;
import org.apache.tika.parser.hwp.HwpV5Parser;
import org.apache.tika.parser.image.BPGParser;
import org.apache.tika.parser.image.HeifParser;
import org.apache.tika.parser.image.ICNSParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.image.JXLParser;
import org.apache.tika.parser.image.JpegParser;
import org.apache.tika.parser.image.PSDParser;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.parser.image.WebPParser;
import org.apache.tika.parser.indesign.IDMLParser;
import org.apache.tika.parser.iptc.IptcAnpaParser;
import org.apache.tika.parser.iwork.IWorkPackageParser;
import org.apache.tika.parser.iwork.iwana.IWork13PackageParser;
import org.apache.tika.parser.iwork.iwana.IWork18PackageParser;
import org.apache.tika.parser.mail.RFC822Parser;
import org.apache.tika.parser.mat.MatParser;
import org.apache.tika.parser.mbox.MboxParser;
import org.apache.tika.parser.microsoft.EMFParser;
import org.apache.tika.parser.microsoft.JackcessParser;
import org.apache.tika.parser.microsoft.MSOwnerFileParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.OldExcelParser;
import org.apache.tika.parser.microsoft.TNEFParser;
import org.apache.tika.parser.microsoft.WMFParser;
import org.apache.tika.parser.microsoft.activemime.ActiveMimeParser;
import org.apache.tika.parser.microsoft.chm.ChmParser;
import org.apache.tika.parser.microsoft.libpst.LibPstParser;
import org.apache.tika.parser.microsoft.onenote.OneNoteParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.microsoft.ooxml.xwpf.ml2006.Word2006MLParser;
import org.apache.tika.parser.microsoft.pst.OutlookPSTParser;
import org.apache.tika.parser.microsoft.pst.PSTMailItemParser;
import org.apache.tika.parser.microsoft.rtf.RTFParser;
import org.apache.tika.parser.microsoft.xml.SpreadsheetMLParser;
import org.apache.tika.parser.microsoft.xml.WordMLParser;
import org.apache.tika.parser.mif.MIFParser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.odf.FlatOpenDocumentParser;
import org.apache.tika.parser.odf.OpenDocumentContentParser;
import org.apache.tika.parser.odf.OpenDocumentMetaParser;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pkg.CompressorParser;
import org.apache.tika.parser.pkg.PackageParser;
import org.apache.tika.parser.pkg.RarParser;
import org.apache.tika.parser.pkg.UnrarParser;
import org.apache.tika.parser.prt.PRTParser;
import org.apache.tika.parser.sas.SAS7BDATParser;
import org.apache.tika.parser.strings.Latin1StringsParser;
import org.apache.tika.parser.strings.StringsParser;
import org.apache.tika.parser.tmx.TMXParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.parser.video.FLVParser;
import org.apache.tika.parser.wacz.WACZParser;
import org.apache.tika.parser.warc.WARCParser;
import org.apache.tika.parser.wordperfect.QuattroProParser;
import org.apache.tika.parser.wordperfect.WordPerfectParser;
import org.apache.tika.parser.xliff.XLIFF12Parser;
import org.apache.tika.parser.xliff.XLZParser;
import org.apache.tika.parser.xml.DcXMLParser;
import org.apache.tika.parser.xml.FictionBookParser;
import org.apache.tika.parser.xml.TextAndAttributeXMLParser;
import org.apache.tika.parser.xml.XMLParser;
import org.apache.tika.parser.xml.XMLProfiler;
import org.gagravarr.tika.FlacParser;
import org.gagravarr.tika.OggParser;
import org.gagravarr.tika.OpusParser;
import org.gagravarr.tika.SpeexParser;
import org.gagravarr.tika.TheoraParser;
import org.gagravarr.tika.VorbisParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class TikaMetadataExtractor {

  public static final Logger LOGGER = LoggerFactory.getLogger(TikaMetadataExtractor.class);

  public static final String METADATA_LIMIT_REACHED_MSG =
      "Document metadata limit reached. To prevent this, increase the limit.";

  private final BodyAndMetadataContentHandler bodyAndMetadataContentHandler;

  private Metadata metadata;

  /**
   * Constructs a new tika extractor which parses the provided input stream into a tika Metadata
   * object, the body text, and the metadata XML
   *
   * @param inputStream - the input stream to be parsed
   * @throws TikaException - if parsing fails
   */
  public TikaMetadataExtractor(InputStream inputStream) throws TikaException {
    this(inputStream, -1, -1);
  }

  /**
   * Constructs a new tika extractor which parses the provided input stream into a tika Metadata
   * object, the body text, and the metadata XML The body text is truncated after maxLength
   *
   * @param inputStream - the input stream to be parsed
   * @param maxBodyLength - the max length of the parsed body text
   * @param maxMetadataLength - the max length of the parsed metadata.
   * @throws TikaException - if parsing fails
   */
  public TikaMetadataExtractor(InputStream inputStream, int maxBodyLength, int maxMetadataLength)
      throws TikaException {
    notNull(inputStream);
    this.metadata = new Metadata();
    this.bodyAndMetadataContentHandler =
        new BodyAndMetadataContentHandler(maxBodyLength, maxMetadataLength);
    parseMetadata(inputStream);
  }

  private void parseMetadata(InputStream inputStream) throws TikaException {

    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Object>)
              () -> {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
//                  Thread.currentThread()
//                      .setContextClassLoader(AutoDetectParser.class.getClassLoader());
                  Parser parser =
                      new AutoDetectParser(
//                          new MidiParser(),
//                          new RTFParser(),
//                          new WARCParser(),
//                          new WACZParser(),
//                          new PListParser(),
//                          new ClassParser(),
//                          new AdobeFontMetricParser(),
//                          new XMLParser(),
//                          new OpenDocumentMetaParser(),
//                          new DcXMLParser(),
//                          new OPFParser(),
//                          new FictionBookParser(),
//                          new TextAndAttributeXMLParser(),
//                          new XLIFF12Parser(),
//                          new TrueTypeParser(),
//                          new WMFParser(),
//                          new HttpParser(),
//                          new StringsParser(),
//                          new XLZParser(),
//                          new RFC822Parser(),
//                          new DIFParser(),
//                          new HwpV5Parser(),
//                          new ErrorParser(),
//                          new FeedParser(),
//                          new OldExcelParser(),
//                          new Word2006MLParser(),
//                          new OfficeParser(),
//                          new OOXMLParser(),
//                          new UnrarParser(),
//                          new MP4Parser(),
//                          new CompositeParser(),
//                          new CompositeExternalParser(),
//                          new DefaultParser(),
//                          new ExternalParser(),
//                          new IptcAnpaParser(),
//                          new MatParser(),
//                          new EMFParser(),
//                          new WordPerfectParser(),
//                          new SAS7BDATParser(),
//                          new EpubContentParser(),
//                          new JackcessParser(),
//                          new MboxParser(),
//                          new Latin1StringsParser(),
//                          new ICNSParser(),
//                          new WebPParser(),
//                          new WordMLParser(),
//                          new SpreadsheetMLParser(),
//                          new org.apache.tika.parser.external.ExternalParser(),
//                          new FlatOpenDocumentParser(),
//                          new LibPstParser(),
//                          new Pkcs7Parser(),
//                          new MSOwnerFileParser(),
//                          new JXLParser(),
//                          new ImageParser(),
//                          new JpegParser(),
//                          new BPGParser(),
//                          new HeifParser(),
//                          new TiffParser(),
//                          new XMLProfiler(),
//                          new TesseractOCRParser(),
//                          new AppleSingleFileParser(),
//                          new IDMLParser(),
//                          new ActiveMimeParser(),
//                          new OpenDocumentContentParser(),
//                          new CompressorParser(),
//                          new FlacParser(),
//                          new VorbisParser(),
//                          new OpusParser(),
//                          new SpeexParser(),
//                          new TheoraParser(),
//                          new OggParser(),
//                          new DelegatingParser(),
//                          new DGN8Parser(),
//                          new TNEFParser(),
//                          new DBFParser(),
//                          new IWork18PackageParser(),
//                          new OutlookPSTParser(),
//                          new PackageParser(),
//                          new TXTParser(),
//                          new SourceCodeParser(),
//                          new MIFParser(),
//                          new JSoupParser(),
//                          new TextAndCSVParser(),
//                          new FLVParser(),
//                          new PRTParser(),
//                          new RarParser(),
//                          new EmptyParser(),
//                          new TMXParser(),
//                          new IWorkPackageParser(),
//                          new OneNoteParser(),
//                          new IWork13PackageParser(),
//                          new ExecutableParser(),
//                          new ChmParser(),
//                          new PSDParser(),
//                          new PSTMailItemParser(),
//                          new AudioParser(),
//                          new EpubParser(),
//                          new PDFParser()//,
//                          new OpenDocumentParser(),
//                          new UniversalExecutableParser(),
//                          new Mp3Parser(),
//                          new QuattroProParser(),
//                          new ForkParser(),
//                          new TSDParser(),
//                          new DWGParser(),
//                          new DWGReadParser()
                      );
                  parser.parse(
                      inputStream,
                      this.bodyAndMetadataContentHandler,
                      metadata,
                      new ParseContext());
                } catch (Exception e) {
                  LOGGER.warn("tika failed", e);
                  throw e;
                } finally {
                  Thread.currentThread().setContextClassLoader(tccl);
                }
                return null;
              });
    } catch (PrivilegedActionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw new TikaException("Unexpected IOException. Stream may already be closed", cause);
      } else if (cause instanceof SAXException) {
        LOGGER.debug("Unexpected tika parsing failure", cause);
      } else {
                  LOGGER.warn("tika failed", e);
      }
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public String getBodyText() {
    return bodyAndMetadataContentHandler.getBodyText();
  }

  public String getMetadataXml() {
    return bodyAndMetadataContentHandler.getMetadataText();
  }

  public Metadata getMetadata() {
    return metadata;
  }
}
