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
package ddf.catalog.filter.proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.geotools.data.DataAccessFactory;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.FunctionExpression;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.filter.function.DefaultFunctionFactory;
import org.geotools.filter.function.PropertyExistsFunction;
import org.geotools.referencing.factory.ReferencingObjectFactory;
import org.geotools.referencing.factory.gridshift.ClasspathGridShiftLocator;
import org.geotools.referencing.factory.gridshift.GridShiftLocator;
import org.geotools.referencing.operation.MathTransformProvider;
import org.geotools.util.ConverterFactory;
import org.geotools.util.factory.FactoryIteratorProvider;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CRSFactory;

public class GeotoolsPluginLoader {

  public GeotoolsPluginLoader() {
    GeoTools.addClassLoader(DefaultFunctionFactory.class.getClassLoader());
    GeoTools.addClassLoader(CRSFactory.class.getClassLoader());
    GeoTools.addFactoryIteratorProvider(
        new FactoryIteratorProvider() {
          @Override
          public <T> Iterator<T> iterator(Class<T> aClass) {
            if (FunctionExpression.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Collections.singletonList(new PropertyExistsFunction()).iterator();
            } else if (FunctionFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Collections.singletonList(new DefaultFunctionFactory()).iterator();
            } else if (PropertyAccessorFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(
                          new org.geotools.filter.expression.SimpleFeaturePropertyAccessorFactory(),
                          new org.geotools.filter.expression.ThisPropertyAccessorFactory(),
                          new org.geotools.filter.expression.DirectPropertyAccessorFactory())
                      .iterator();
            } else if (ConverterFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(
                          new org.geotools.data.util.CommonsConverterFactory(),
                          new org.geotools.data.util.NumericConverterFactory(),
                          new org.geotools.data.util.PercentageConverterFactory(),
                          new org.geotools.data.util.GeometryConverterFactory(),
                          new org.geotools.data.util.GeometryTypeConverterFactory(),
                          new org.geotools.data.util.TemporalConverterFactory(),
                          new org.geotools.data.util.BooleanConverterFactory(),
                          new org.geotools.data.util.ColorConverterFactory(),
                          new org.geotools.data.util.CollectionConverterFactory(),
                          new org.geotools.data.util.CharsetConverterFactory(),
                          new org.geotools.data.util.UuidConverterFactory(),
                          new org.geotools.data.util.EnumerationConverterFactory(),
                          new org.geotools.data.util.QNameConverterFactory(),
                          new org.geotools.data.util.MeasureConverterFactory(),
                          new org.geotools.temporal.TemporalConverterFactory(),
                          new org.geotools.data.util.NameConverterFactory(),
                          new org.geotools.data.util.ArrayConverterFactory(),
                          new org.geotools.data.util.ComplexAttributeConverterFactory(),
                          new org.geotools.data.util.CRSConverterFactory(),
                          new org.geotools.data.util.InterpolationConverterFactory(),
                          new org.geotools.data.util.LobConverterFactory())
                      .iterator();
            } else if (FeatureFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(
                          new org.geotools.feature.LenientFeatureFactoryImpl(),
                          new org.geotools.feature.ValidatingFeatureFactoryImpl())
                      .iterator();
            } else if (FilterFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>) Collections.singletonList(new FilterFactoryImpl()).iterator();
            } else if (FeatureTypeFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Collections.singletonList(new FeatureTypeFactoryImpl()).iterator();
            } else if (Function.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(
                          new org.geotools.filter.AreaFunction(),
                          new org.geotools.filter.LengthFunction(),
                          new org.geotools.filter.function.AttributeCountFunction(),
                          new org.geotools.filter.function.CategorizeFunction(),
                          new org.geotools.filter.function.ClassifyFunction(),
                          new org.geotools.filter.function.EqualIntervalFunction(),
                          new org.geotools.filter.function.StandardDeviationFunction(),
                          new org.geotools.filter.function.QuantileFunction(),
                          new org.geotools.filter.function.EqualAreaFunction(),
                          new org.geotools.filter.function.UniqueIntervalFunction(),
                          new org.geotools.filter.function.Collection_AverageFunction(),
                          new org.geotools.filter.function.Collection_BoundsFunction(),
                          new org.geotools.filter.function.Collection_CountFunction(),
                          new org.geotools.filter.function.Collection_MinFunction(),
                          new org.geotools.filter.function.Collection_MedianFunction(),
                          new org.geotools.filter.function.Collection_MaxFunction(),
                          new org.geotools.filter.function.Collection_NearestFunction(),
                          new org.geotools.filter.function.Collection_SumFunction(),
                          new org.geotools.filter.function.Collection_UniqueFunction(),
                          new org.geotools.filter.function.EnvFunction(),
                          new org.geotools.filter.function.StringTemplateFunction(),
                          new org.geotools.filter.function.FilterFunction_contains(),
                          new org.geotools.filter.function.FilterFunction_isEmpty(),
                          new org.geotools.filter.function.FilterFunction_parseDouble(),
                          new org.geotools.filter.function.FilterFunction_parseInt(),
                          new org.geotools.filter.function.FilterFunction_parseLong(),
                          new org.geotools.filter.function.FilterFunction_intersects(),
                          new org.geotools.filter.function.FilterFunction_isClosed(),
                          new org.geotools.filter.function.FilterFunction_geomFromWKT(),
                          new org.geotools.filter.function.FilterFunction_setCRS(),
                          new org.geotools.filter.function.FilterFunction_toWKT(),
                          new org.geotools.filter.function.FilterFunction_geomLength(),
                          new org.geotools.filter.function.FilterFunction_isValid(),
                          new org.geotools.filter.function.FilterFunction_geometryType(),
                          new org.geotools.filter.function.FilterFunction_numPoints(),
                          new org.geotools.filter.function.FilterFunction_isSimple(),
                          new org.geotools.filter.function.FilterFunction_distance(),
                          new org.geotools.filter.function.FilterFunction_isWithinDistance(),
                          new org.geotools.filter.function.FilterFunction_area(),
                          new org.geotools.filter.function.FilterFunction_centroid(),
                          new org.geotools.filter.function.FilterFunction_interiorPoint(),
                          new org.geotools.filter.function.FilterFunction_dimension(),
                          new org.geotools.filter.function.FilterFunction_boundary(),
                          new org.geotools.filter.function.FilterFunction_boundaryDimension(),
                          new org.geotools.filter.function.FilterFunction_envelope(),
                          new org.geotools.filter.function.FilterFunction_disjoint(),
                          new org.geotools.filter.function.FilterFunction_touches(),
                          new org.geotools.filter.function.FilterFunction_crosses(),
                          new org.geotools.filter.function.FilterFunction_within(),
                          new org.geotools.filter.function.FilterFunction_overlaps(),
                          new org.geotools.filter.function.FilterFunction_relatePattern(),
                          new org.geotools.filter.function.FilterFunction_relate(),
                          new org.geotools.filter.function.FilterFunction_bufferWithSegments(),
                          new org.geotools.filter.function.FilterFunction_buffer(),
                          new org.geotools.filter.function.FilterFunction_convexHull(),
                          new org.geotools.filter.function.FilterFunction_intersection(),
                          new org.geotools.filter.function.FilterFunction_union(),
                          new org.geotools.filter.function.FilterFunction_difference(),
                          new org.geotools.filter.function.FilterFunction_symDifference(),
                          new org.geotools.filter.function.FilterFunction_equalsExactTolerance(),
                          new org.geotools.filter.function.FilterFunction_equalsExact(),
                          new org.geotools.filter.function.FilterFunction_numGeometries(),
                          new org.geotools.filter.function.FilterFunction_getGeometryN(),
                          new org.geotools.filter.function.FilterFunction_getX(),
                          new org.geotools.filter.function.FilterFunction_getY(),
                          new org.geotools.filter.function.FilterFunction_getZ(),
                          new org.geotools.filter.function.FilterFunction_pointN(),
                          new org.geotools.filter.function.FilterFunction_startAngle(),
                          new org.geotools.filter.function.FilterFunction_startPoint(),
                          new org.geotools.filter.function.FilterFunction_endPoint(),
                          new org.geotools.filter.function.FilterFunction_endAngle(),
                          new org.geotools.filter.function.FilterFunction_isRing(),
                          new org.geotools.filter.function.FilterFunction_exteriorRing(),
                          new org.geotools.filter.function.FilterFunction_numInteriorRing(),
                          new org.geotools.filter.function.FilterFunction_minimumCircle(),
                          new org.geotools.filter.function.FilterFunction_minimumRectangle(),
                          new org.geotools.filter.function.FilterFunction_interiorRingN(),
                          new org.geotools.filter.function.FilterFunction_octagonalEnvelope(),
                          new org.geotools.filter.function.FilterFunction_minimumDiameter(),
                          new org.geotools.filter.function.FilterFunction_strConcat(),
                          new org.geotools.filter.function.FilterFunction_strEndsWith(),
                          new org.geotools.filter.function.FilterFunction_strStartsWith(),
                          new org.geotools.filter.function.FilterFunction_strCapitalize(),
                          new org.geotools.filter.function.FilterFunction_strAbbreviate(),
                          new org.geotools.filter.function.FilterFunction_strDefaultIfBlank(),
                          new org.geotools.filter.function.FilterFunction_strStripAccents(),
                          new org.geotools.filter.function.FilterFunction_strEqualsIgnoreCase(),
                          new org.geotools.filter.function.FilterFunction_strIndexOf(),
                          new org.geotools.filter.function.FilterFunction_strLastIndexOf(),
                          new org.geotools.filter.function.FilterFunction_strLength(),
                          new org.geotools.filter.function.FilterFunction_strToLowerCase(),
                          new org.geotools.filter.function.FilterFunction_strToUpperCase(),
                          new org.geotools.filter.function.FilterFunction_strMatches(),
                          new org.geotools.filter.function.FilterFunction_strPosition(),
                          new org.geotools.filter.function.FilterFunction_strReplace(),
                          new org.geotools.filter.function.FilterFunction_strSubstring(),
                          new org.geotools.filter.function.FilterFunction_strSubstringStart(),
                          new org.geotools.filter.function.FilterFunction_strTrim(),
                          new org.geotools.filter.function.FilterFunction_strTrim2(),
                          new org.geotools.filter.function.FilterFunction_parseBoolean(),
                          new org.geotools.filter.function.FilterFunction_roundDouble(),
                          new org.geotools.filter.function.FilterFunction_int2ddouble(),
                          new org.geotools.filter.function.FilterFunction_int2bbool(),
                          new org.geotools.filter.function.FilterFunction_double2bool(),
                          new org.geotools.filter.function.FilterFunction_if_then_else(),
                          new org.geotools.filter.function.FilterFunction_equalTo(),
                          new org.geotools.filter.function.FilterFunction_notEqualTo(),
                          new org.geotools.filter.function.FilterFunction_lessThan(),
                          new org.geotools.filter.function.FilterFunction_greaterThan(),
                          new org.geotools.filter.function.FilterFunction_greaterEqualThan(),
                          new org.geotools.filter.function.FilterFunction_lessEqualThan(),
                          new org.geotools.filter.function.FilterFunction_isLike(),
                          new org.geotools.filter.function.FilterFunction_isNull(),
                          new org.geotools.filter.function.FilterFunction_between(),
                          new org.geotools.filter.function.FilterFunction_not(),
                          new org.geotools.filter.function.InFunction(),
                          new org.geotools.filter.function.FilterFunction_in2(),
                          new org.geotools.filter.function.FilterFunction_in3(),
                          new org.geotools.filter.function.FilterFunction_in4(),
                          new org.geotools.filter.function.FilterFunction_in5(),
                          new org.geotools.filter.function.FilterFunction_in6(),
                          new org.geotools.filter.function.FilterFunction_in7(),
                          new org.geotools.filter.function.FilterFunction_in8(),
                          new org.geotools.filter.function.FilterFunction_in9(),
                          new org.geotools.filter.function.FilterFunction_in10(),
                          new org.geotools.filter.function.FilterFunction_dateParse(),
                          new org.geotools.filter.function.FilterFunction_dateFormat(),
                          new org.geotools.filter.function.FilterFunction_numberFormat(),
                          new org.geotools.filter.function.FilterFunction_numberFormat2(),
                          new org.geotools.filter.function.FilterFunction_Convert(),
                          new org.geotools.filter.function.FilterFunction_vertices(),
                          new org.geotools.filter.function.FilterFunction_offset(),
                          new org.geotools.filter.function.FilterFunction_isometric(),
                          new org.geotools.filter.function.FilterFunction_property(),
                          new org.geotools.filter.function.FilterFunction_distance3D(),
                          new org.geotools.filter.function.FilterFunction_isWithinDistance3D(),
                          new org.geotools.filter.function.FilterFunction_intersects3D(),
                          new org.geotools.filter.function.FilterFunction_disjoint3D(),
                          new org.geotools.filter.function.GeometryFunction(),
                          new org.geotools.filter.function.IDFunction(),
                          new org.geotools.filter.function.InterpolateFunction(),
                          new org.geotools.filter.function.RecodeFunction(),
                          new org.geotools.filter.function.math.FilterFunction_IEEEremainder(),
                          new org.geotools.filter.function.math.FilterFunction_abs(),
                          new org.geotools.filter.function.math.FilterFunction_abs_2(),
                          new org.geotools.filter.function.math.FilterFunction_abs_3(),
                          new org.geotools.filter.function.math.FilterFunction_abs_4(),
                          new org.geotools.filter.function.math.FilterFunction_acos(),
                          new org.geotools.filter.function.math.FilterFunction_asin(),
                          new org.geotools.filter.function.math.FilterFunction_atan(),
                          new org.geotools.filter.function.math.FilterFunction_atan2(),
                          new org.geotools.filter.function.math.FilterFunction_ceil(),
                          new org.geotools.filter.function.math.FilterFunction_cos(),
                          new org.geotools.filter.function.math.FilterFunction_exp(),
                          new org.geotools.filter.function.math.FilterFunction_floor(),
                          new org.geotools.filter.function.math.FilterFunction_log(),
                          new org.geotools.filter.function.math.FilterFunction_max(),
                          new org.geotools.filter.function.math.FilterFunction_max_2(),
                          new org.geotools.filter.function.math.FilterFunction_max_3(),
                          new org.geotools.filter.function.math.FilterFunction_max_4(),
                          new org.geotools.filter.function.math.FilterFunction_min(),
                          new org.geotools.filter.function.math.FilterFunction_min_2(),
                          new org.geotools.filter.function.math.FilterFunction_min_3(),
                          new org.geotools.filter.function.math.FilterFunction_min_4(),
                          new org.geotools.filter.function.math.FilterFunction_pow(),
                          new org.geotools.filter.function.math.FilterFunction_random(),
                          new org.geotools.filter.function.math.FilterFunction_rint(),
                          new org.geotools.filter.function.math.FilterFunction_round(),
                          new org.geotools.filter.function.math.FilterFunction_round_2(),
                          new org.geotools.filter.function.math.FilterFunction_sin(),
                          new org.geotools.filter.function.math.FilterFunction_sqrt(),
                          new org.geotools.filter.function.math.FilterFunction_tan(),
                          new org.geotools.filter.function.math.FilterFunction_toDegrees(),
                          new org.geotools.filter.function.math.FilterFunction_toRadians(),
                          new org.geotools.filter.function.math.ModuloFunction(),
                          new org.geotools.filter.function.math.PiFunction(),
                          new org.geotools.filter.function.PropertyExistsFunction(),
                          new org.geotools.filter.function.string.ConcatenateFunction(),
                          new org.geotools.filter.function.string.URLEncodeFunction(),
                          new org.geotools.filter.function.JenksNaturalBreaksFunction(),
                          new org.geotools.filter.function.FilterFunction_listMultiply(),
                          new org.geotools.filter.function.FilterFunction_list(),
                          new org.geotools.styling.visitor.RescaleToPixelsFunction(),
                          new org.geotools.filter.function.color.HSLFunction(),
                          new org.geotools.filter.function.color.SaturateFunction(),
                          new org.geotools.filter.function.color.DesaturateFunction(),
                          new org.geotools.filter.function.color.DarkenFunction(),
                          new org.geotools.filter.function.color.LightenFunction(),
                          new org.geotools.filter.function.color.SpinFunction(),
                          new org.geotools.filter.function.color.MixFunction(),
                          new org.geotools.filter.function.color.TintFunction(),
                          new org.geotools.filter.function.color.ShadeFunction(),
                          new org.geotools.filter.function.color.GrayscaleFunction(),
                          new org.geotools.filter.function.color.ConstrastFunction(),
                          new org.geotools.filter.function.BoundedByFunction(),
                          new org.geotools.filter.function.DateDifferenceFunction(),
                          new org.geotools.filter.function.JsonPointerFunction())
                      .iterator();
            } else if (GridShiftLocator.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Collections.singletonList(new ClasspathGridShiftLocator()).iterator();
            } else if (MathTransformProvider.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(
                          new org.geotools.referencing.operation.transform.LogarithmicTransform1D
                              .Provider(),
                          new org.geotools.referencing.operation.transform.ExponentialTransform1D
                              .Provider(),
                          new org.geotools.referencing.operation.transform.ProjectiveTransform
                              .ProviderAffine(),
                          new org.geotools.referencing.operation.transform.ProjectiveTransform
                              .ProviderLongitudeRotation(),
                          new org.geotools.referencing.operation.transform.GeocentricTranslation
                              .Provider(),
                          new org.geotools.referencing.operation.transform.GeocentricTranslation
                              .ProviderSevenParam(),
                          new org.geotools.referencing.operation.transform.GeocentricTranslation
                              .ProviderFrameRotation(),
                          new org.geotools.referencing.operation.transform.GeocentricTransform
                              .Provider(),
                          new org.geotools.referencing.operation.transform.GeocentricTransform
                              .ProviderInverse(),
                          new org.geotools.referencing.operation.transform.MolodenskiTransform
                              .Provider(),
                          new org.geotools.referencing.operation.transform.MolodenskiTransform
                              .ProviderAbridged(),
                          new org.geotools.referencing.operation.transform.NADCONTransform
                              .Provider(),
                          new org.geotools.referencing.operation.transform.NTv2Transform.Provider(),
                          new org.geotools.referencing.operation.transform
                              .SimilarityTransformProvider(),
                          new org.geotools.referencing.operation.transform
                              .WarpTransform2DProvider(),
                          new org.geotools.referencing.operation.transform.EarthGravitationalModel
                              .Provider(),
                          new org.geotools.referencing.operation.projection.EquidistantCylindrical
                              .Provider(),
                          new org.geotools.referencing.operation.projection.EquidistantCylindrical
                              .SphericalProvider(),
                          new org.geotools.referencing.operation.projection.PlateCarree.Provider(),
                          new org.geotools.referencing.operation.projection.Mercator1SP.Provider(),
                          new org.geotools.referencing.operation.projection.Mercator2SP.Provider(),
                          new org.geotools.referencing.operation.projection
                              .MercatorPseudoProvider(),
                          new org.geotools.referencing.operation.projection.TransverseMercator
                              .Provider(),
                          new org.geotools.referencing.operation.projection.TransverseMercator
                              .Provider_SouthOrientated(),
                          new org.geotools.referencing.operation.projection.ObliqueMercator
                              .Provider(),
                          new org.geotools.referencing.operation.projection.ObliqueMercator
                              .Provider_TwoPoint(),
                          new org.geotools.referencing.operation.projection.HotineObliqueMercator
                              .Provider(),
                          new org.geotools.referencing.operation.projection.HotineObliqueMercator
                              .Provider_TwoPoint(),
                          new org.geotools.referencing.operation.projection.AlbersEqualArea
                              .Provider(),
                          new org.geotools.referencing.operation.projection.LambertConformal1SP
                              .Provider(),
                          new org.geotools.referencing.operation.projection.LambertConformal2SP
                              .Provider(),
                          new org.geotools.referencing.operation.projection
                              .LambertConformalEsriProvider(),
                          new org.geotools.referencing.operation.projection.LambertConformalBelgium
                              .Provider(),
                          new org.geotools.referencing.operation.projection
                              .LambertAzimuthalEqualArea.Provider(),
                          new org.geotools.referencing.operation.projection.Orthographic.Provider(),
                          new org.geotools.referencing.operation.projection.Stereographic
                              .Provider(),
                          new org.geotools.referencing.operation.projection.ObliqueStereographic
                              .Provider(),
                          new org.geotools.referencing.operation.projection.PolarStereographic
                              .ProviderA(),
                          new org.geotools.referencing.operation.projection.PolarStereographic
                              .ProviderB(),
                          new org.geotools.referencing.operation.projection.PolarStereographic
                              .ProviderNorth(),
                          new org.geotools.referencing.operation.projection.PolarStereographic
                              .ProviderSouth(),
                          new org.geotools.referencing.operation.projection.NewZealandMapGrid
                              .Provider(),
                          new org.geotools.referencing.operation.projection.Krovak.Provider(),
                          new org.geotools.referencing.operation.projection.CassiniSoldner
                              .Provider(),
                          new org.geotools.referencing.operation.projection.EquidistantConic
                              .Provider(),
                          new org.geotools.referencing.operation.projection.Polyconic.Provider(),
                          new org.geotools.referencing.operation.projection.Robinson.Provider(),
                          new org.geotools.referencing.operation.projection.WinkelTripel
                              .WinkelProvider(),
                          new org.geotools.referencing.operation.projection.WinkelTripel
                              .AitoffProvider(),
                          new org.geotools.referencing.operation.projection.EckertIV.Provider(),
                          new org.geotools.referencing.operation.projection.Mollweide
                              .MollweideProvider(),
                          new org.geotools.referencing.operation.projection.Mollweide
                              .WagnerIVProvider(),
                          new org.geotools.referencing.operation.projection.Mollweide
                              .WagnerVProvider(),
                          new org.geotools.referencing.operation.projection.Gnomonic.Provider(),
                          new org.geotools.referencing.operation.projection.WorldVanDerGrintenI
                              .Provider(),
                          new org.geotools.referencing.operation.projection.Sinusoidal.Provider(),
                          new org.geotools.referencing.operation.projection.GeneralOblique
                              .Provider(),
                          new org.geotools.referencing.operation.projection.MeteosatSG.Provider(),
                          new org.geotools.referencing.operation.projection.GeostationarySatellite
                              .Provider(),
                          new org.geotools.referencing.operation.projection.RotatedPole.Provider(),
                          new org.geotools.referencing.operation.projection.AzimuthalEquidistant
                              .Provider(),
                          new org.geotools.referencing.operation.projection.CylindricalEqualArea
                              .Provider(),
                          new org.geotools.referencing.operation.projection.CylindricalEqualArea
                              .BehrmannProvider(),
                          new org.geotools.referencing.operation.projection.CylindricalEqualArea
                              .LambertCylindricalEqualAreaProvider(),
                          new org.geotools.referencing.operation.projection.EqualArea.Provider())
                      .iterator();
            } else if (CRSAuthorityFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(
                          new org.geotools.referencing.factory.epsg.FactoryUsingWKT(),
                          new org.geotools.referencing.factory.epsg.LongitudeFirstFactory(),
                          new org.geotools.referencing.factory.epsg.CartesianAuthorityFactory(),
                          new org.geotools.referencing.factory.wms.AutoCRSFactory(),
                          new org.geotools.referencing.factory.wms.WebCRSFactory(),
                          new org.geotools.referencing.factory.URN_AuthorityFactory(),
                          new org.geotools.referencing.factory.HTTP_AuthorityFactory(),
                          new org.geotools.referencing.factory.HTTP_URI_AuthorityFactory())
                      .iterator();
            } else if (CRSFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Collections.singletonList(new ReferencingObjectFactory()).iterator();
            } else if (DataAccessFactory.class.isAssignableFrom(aClass)) {
              return (Iterator<T>)
                  Arrays.asList(new org.geotools.data.shapefile.ShapefileDataStoreFactory())
                      .iterator();
            } else {
              return null;
            }
          }
        });
  }
}
