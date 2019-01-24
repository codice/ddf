/// **
// * Copyright (c) Codice Foundation
// *
// * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
// * Lesser General Public License as published by the Free Software Foundation, either version 3 of
// * the License, or any later version.
// *
// * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
// * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
// the
// * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
// * License is distributed along with this program and can be found at
// * <http://www.gnu.org/licenses/lgpl.html>.
// */
// package ddf.catalog.transformer.xls;
//
// import ddf.catalog.data.BinaryContent;
// import ddf.catalog.data.Metacard;
// import ddf.catalog.data.impl.BinaryContentImpl;
// import ddf.catalog.transform.CatalogTransformerException;
// import ddf.catalog.transform.MetacardTransformer;
// import ddf.catalog.transformer.html.models.HtmlExportCategory;
// import ddf.catalog.transformer.html.models.HtmlMetacardModel;
// import java.io.ByteArrayInputStream;
// import java.io.Serializable;
// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import org.apache.commons.lang.StringUtils;
//
// public class XlsMetacardTransformer implements MetacardTransformer {
//
//    private XlsMetacardUtility htmlMetacardUtility;
//
//    public XlsMetacardTransformer(List<XlsExportCategory> categoryList) {
//        this.htmlMetacardUtility = new XlsMetacardUtility(categoryList);
//    }
//
//    @Override
//    public BinaryContent transform(Metacard metacard, Map<String, Serializable> map)
//            throws CatalogTransformerException {
//
//        if (metacard == null) {
//            throw new CatalogTransformerException("Null metacard cannot be transformed to HTML");
//        }
//
//        List<HtmlExportCategory> categoryList =
//                HtmlMetacardUtility.sortCategoryList(htmlMetacardUtility.getCategoryList());
//
//        List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
//        metacardModelList.add(new HtmlMetacardModel(metacard, categoryList));
//
//        String html = htmlMetacardUtility.buildHtml(metacardModelList);
//
//        if (StringUtils.isEmpty(html)) {
//            throw new CatalogTransformerException("Metacard cannot be transformed to HTML");
//        }
//
//        return new BinaryContentImpl(
//                new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)),
//                htmlMetacardUtility.getMimeType());
//    }
// }
