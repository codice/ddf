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
package org.codice.spock.extension.builtin

import org.codice.spock.extension.Supplemental
import org.spockframework.mock.IMockMethod
import org.spockframework.mock.MockImplementation
import org.spockframework.mock.MockNature
import org.spockframework.mock.MockUtil
import org.spockframework.mock.runtime.MockInvocation
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.extension.builtin.ConfineMetaClassChangesInterceptor
import org.spockframework.runtime.model.SpecInfo
import org.spockframework.util.Nullable
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import spock.mock.MockingApi

import java.lang.reflect.Array
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Provides the extension point for the {@link Supplemental} annotation.
 */
class SupplementalExtension extends AbstractAnnotationDrivenExtension<Supplemental> {
  private static
  def DEFAULT_VALUES = [
      (null): null,
      (void): null,
      (Void): null,
      (boolean): false,
      (Boolean): false,
      (int): 0,
      (Integer): 0,
      (long): 0L,
      (Long): 0L,
      (float): 0.0f,
      (Float): 0.0f,
      (double): 0.0D,
      (Double): 0.0D,
      (char): ('\u0000' as char),
      (Character): ('\u0000' as char),
      (short): (short) 0,
      (Short): (short) 0,
      (byte): (byte) 0,
      (Byte): (byte) 0,
      (String): '',
      (CharSequence): '',
      (StringBuilder): { new StringBuilder() },
      (StringBuffer): { new StringBuffer() },
      (GString): GString.EMPTY,
      (Iterable): { new ArrayList<>() },
      (Collection): { new ArrayList<>() },
      (List): { new ArrayList<>() },
      (Set): { new HashSet<>() },
      (SortedSet): { new TreeSet<>() },
      (NavigableSet): { new TreeSet<>() },
      (Map): { new HashMap<>() },
      (SortedMap): { new TreeMap<>() },
      (NavigableMap): { new TreeMap<>() },
      (Optional): Optional.empty(),
      (BigInteger): BigInteger.ZERO,
      (BigDecimal): BigDecimal.ZERO
  ]

  private def mockFactory = new DetachedMockFactory()

  @Override
  void visitSpecAnnotation(Supplemental annotation, SpecInfo spec) {
    // register the {@link ConfineMetaClassChanges} interceptor first to restore all metaclass changes
    // done by our own interceptor
    spec.bottomSpec.addInterceptor(new ConfineMetaClassChangesInterceptor([Class, Method, System, MockingApi]))

    spec.bottomSpec.addInterceptor(new IMethodInterceptor() {
      @Override
      void intercept(IMethodInvocation invocation) throws Throwable {
        def ext = SupplementalExtension.this

        // supplement the Class class
        Class.metaClass {
          getApiMethods { ext.getApiMethods(delegate) }
          getProxyableMethods { ext.getProxyableMethods(delegate) }
          getMethodBySimplePrototype { String p -> ext.getMethodBySimplePrototype(delegate, p) }
          getNoSpockSimpleName { ext.getNoSpockSimpleName(delegate) }
        }
        // supplement the Method class
        Method.metaClass {
          getSimplePrototype { ext.getSimplePrototype(delegate) }
          verifyInvocation { MockInvocation i, Object... p -> ext.verifyInvocation(delegate, i, p) }
        }
        // supplement the System class
        System.metaClass.static.setPropertyIfNotNull = ext.&setPropertyIfNotNull
        // supplement specification class to allow creating dummies
        invocation.instance.class.metaClass {
          Dummy { Class<?> type -> ext.createDummy(delegate, type) }
          Dummies { Class<?>[] types -> ext.createDummies(delegate, types) }
        }
        invocation.proceed()
      }
    })
  }

  /**
   * Gets all API methods (inherited or not) for the given class filtering away all non public
   * methods and all methods defined by the {@link Object} class (e.g. {@link Object#equals},
   * {@link Object#toString}, {@link Object#clone} ...).
   *
   * @param type the type for which to retrieve all api methods
   * @return a list of all api methods for the given class
   */
  private def getApiMethods(Class<?> type) {
    type.methods.findAll {
      Modifier.isPublic(it.modifiers) && (it.declaringClass != Object.class)
    }
  }

  /**
   * Gets all proxy-able public methods (inherited or not) for the given class filtering away all
   * final methods and all methods defined by the {@link Object} class (e.g. {@link Object#equals},
   * {@link Object#toString}, {@link Object#clone} ...).
   *
   * @param type the type for which to retrieve all proxy-able methods
   * @return a list of all proxy-able methods for the given class
   */
  private def getProxyableMethods(Class<?> type) {
    getApiMethods(type).findAll { !Modifier.isFinal(it.modifiers) }
  }

  /**
   * Returns a {@code Method} object that reflects the specified public member method of the class
   * or interface represented by the specified class that matches the given simple prototype.
   *
   * @param type the type for which to retrieve a method matching the given prototype
   * @param prototype the prototype to match
   * @return the corresponding method
   * @throws NoSuchMethodException if a matching method is not found
   */
  private def getMethodBySimplePrototype(Class<?> type, String prototype) {
    def p = prototype?.replaceAll("\\s", "")
    def method = type.methods.find { getSimplePrototype(it) == p }

    if (method) {
      return method
    }
    throw new NoSuchMethodException(prototype)
  }

  /**
   * Returns the simple name of the specified class as given in the source code stripping away any
   * references to Spock mocks, stubs, or spies. Returns an empty string if the class is anonymous.
   *
   * @param type the class for which to get its simple name
   * @return the simple name of the specified class without any references to Spock mocks, stubs, or
   * spies
   */
  private def getNoSpockSimpleName(Class<?> type) {
    type.simpleName.replaceFirst('\\$Spock.*\\$.*$', '')
  }

  /**
   * Gets a simple prototype string to represent the specified method.
   *
   * @param method the method for which to get a simple prototype
   * @return a corresponding simple prototype
   */
  private def getSimplePrototype(Method method) {
    method.name + '(' + method.parameterTypes*.simpleName.join(',') + ')'
  }

  /**
   * Gets a simple prototype string to represent the specified method.
   *
   * @param method the method for which to get a simple prototype
   * @return a corresponding simple prototype
   */
  private def getSimplePrototype(IMockMethod method) {
    method.name + '(' + method.parameterTypes*.simpleName.join(',') + ')'
  }

  /**
   * Asserts if the mock invocation matches the specified method. Only the method name and
   * parameter types/values are verified.
   *
   * <p>This method is meant to be invoked from within a closure associated with a stubbed
   * interaction by passing it a reference to the closure's delegate and the expected parameters
   * used when calling the method. Parameters are verified using identity check and not equality.
   * No verification of parameters will occur if no expected parameters are specified.
   *
   * @param method the method expected to be called
   * @param delegate the mock invocation to be verified
   * @param the expected parameters
   * @throws AssertionError if the method name or parameter types or values do not match
   */
  private def verifyInvocation(Method method, MockInvocation delegate, Object... parameters) {
    def methodPrototype = getSimplePrototype(method)
    def delegatePrototype = getSimplePrototype(delegate.method)

    assert methodPrototype == delegatePrototype: "expecting $methodPrototype to be invoked instead of $delegatePrototype"
    assert method.name == delegate.method.name
    assert method.parameterTypes == delegate.method.parameterTypes
    if (parameters.length) {
      def delegate_arguments = delegate.arguments

      for (def i = 0; i < method.parameters.length; i++) {
        assert delegate_arguments[i].is(parameters[i])
      }
    }
  }

  /**
   * Sets the system property indicated by the specified key only if the specified value is
   * not <code>null</code> otherwise clear any mappings to the specified key.
   *
   * @param name the name of the system property to be set or removed
   * @param value the new value for the system property or <code>null</code> to clear the mapping
   * @return the previous value of the system property, or <code>null</code> if it did not have one
   */
  private def setPropertyIfNotNull(String name, @Nullable String value) {
    (value != null) ? System.setProperty(name, value) : System.clearProperty(name)
  }

  /**
   * Creates a dummy value or stub for the specified type.
   *
   * @param type the type for which to create a dummy value or stub
   * @return a corresponding default value or stub
   */
  private def <T> T createDummy(Specification spec, Class<T> type) {
    // see org.spockframework.mock.EmptyOrDummyResponse
    def val = DEFAULT_VALUES.getOrDefault(type, type)

    if (val instanceof Closure) {
      val.call()
    } else if (!val.is(type)) {
      // if not the default value; it was defined in DEFAULT_VALUES
      val
    } else if (type.array) {
      Array.newInstance(type.componentType, 0)
    } else if (type.enum) {
      def constants = type.enumConstants

      (constants.length > 0) ? constants[0] : null
    } else {
      try {
        type.newInstance()
      } catch (Exception e) {
        // if we cannot instantiate it, then fallback to stubbing the class
        if (type.classLoader == spec.class.classLoader) {
          (T) mockFactory.Stub(type)
        } else {
          // this is to work around a bug where when we use the Definalizer, the spec ends up living
          // in a different classloader than standard Java classes, as such, when we attempt to create
          // stubs the normal way, it aborts since it is trying to add the ISpockMockObject interface
          // to the stubbed class which is not visible from the parent class loader
          (T) new MockUtil().createDetachedMock(
              type.simpleName,
              type,
              MockNature.STUB,
              MockImplementation.JAVA,
              Collections.<String, Object> emptyMap(),
              spec.class.classLoader
          )
        }
      }
    }
  }

  /**
   * Creates dummy default values or stubs for the specified types.
   *
   * @param types the types for which to create dummy default values or stubs
   * @return a corresponding array of dummy default values or stubs corresponding to the provided types
   */
  private def createDummies(Specification spec, Class<?>... types) {
    types.collect { type -> this.createDummy(spec, type) }
  }
}
