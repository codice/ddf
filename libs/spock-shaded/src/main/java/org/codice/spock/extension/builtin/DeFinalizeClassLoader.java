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
package org.codice.spock.extension.builtin;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import org.codice.spock.extension.DeFinalize;

/**
 * Defines the classloader used by the {@link DeFinalizeSputnik} test runner. This classloader is
 * designed with an aggressive strategy where it will load all classes first before delegating to
 * its parent. This classloader will therefore reload all classes while definalizing those that are
 * requested except for all classes in the following packages:
 *
 * <ul>
 *   <li>java
 *   <li>javax
 *   <li>sun
 *   <li>org.xml
 *   <li>org.junit
 * </ul>
 *
 * These packages are not being reloaded as they are required by the {@link DeFinalizeSputnik} test
 * runner to delegate to the real {@link org.spockframework.runtime.Sputnik} test runner.
 *
 * <p>All loaded classes will be done so with the same protection domain used if they had been
 * loaded by the parent classloader.
 */
public class DeFinalizeClassLoader extends ClassLoader {
  private final ClassPool pool;
  private final Set<String> filters;

  /**
   * Constructs a new classloader for the specified Spock test specification class. The class is
   * used to retrieve the {@link DeFinalize} annotations in order to identify which classes or
   * packages should be definalized as they are being loaded.
   *
   * <p>The classloader used to load this classloader class will be defined as its parent
   * classloader.
   *
   * @param specClass the Spock test specification class
   */
  public DeFinalizeClassLoader(Class<?> specClass) {
    super(DeFinalizeClassLoader.class.getClassLoader());
    this.pool = new ClassPool(false);
    this.pool.appendClassPath(new LoaderClassPath(getParent()));
    this.pool.appendSystemPath();
    this.filters =
        Stream.concat(
                Stream.of(specClass.getAnnotationsByType(DeFinalize.class))
                    .map(DeFinalize::value)
                    .flatMap(Stream::of)
                    .map(Class::getName),
                Stream.of(specClass.getAnnotationsByType(DeFinalize.class))
                    .map(DeFinalize::packages)
                    .flatMap(Stream::of))
            .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Loads the class with the specified <a href="#name">binary name</a>.
   *
   * <p>The class will always be loaded first from its parent classloader. It will be returned as is
   * if the class is from one of the reserved packages (see above). Otherwise, the class will be
   * reloaded and optionally definalize if the class match a requested class or package from one of
   * the {@link DeFinalize} annotations of the Spock test specification class. The protection domain
   * from the version of the class loaded by the parent classloader will be re-used when defining
   * the reloaded class.
   *
   * @param name the binary name of the class to load
   * @param resolve <code>true</code> to resolve the class; <code>false</code> otherwise
   * @return the resulting class object
   * @throws ClassNotFoundException if the class could not be found
   */
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> clazz = findLoadedClass(name);

      if (clazz == null) {
        clazz = super.loadClass(name, resolve); // always load it from our parent first
        if (!name.startsWith("java.")
            && !name.startsWith("javax.")
            && !name.startsWith("sun.")
            && !name.startsWith("org.xml.")
            && !name.startsWith("org.junit.")) {
          try {
            boolean definalize = filters.contains(name);

            if (!definalize) {
              for (int i = name.indexOf('.'); i > 0; i = name.indexOf('.', ++i)) {
                if (filters.contains(name.substring(0, i))) {
                  definalize = true;
                  break;
                }
              }
            }
            clazz = reloadClass(clazz, definalize);
          } catch (NotFoundException e) {
            throw new ClassNotFoundException(e.getMessage(), e);
          } catch (CannotCompileException e) {
            throw (ClassFormatError) new ClassFormatError(e.getMessage()).initCause(e);
          }
        }
      }
      if (resolve) {
        resolveClass(clazz);
      }
      return clazz;
    }
  }

  private Class<?> reloadClass(Class<?> clazz, boolean definalize)
      throws NotFoundException, CannotCompileException {
    final CtClass ctClass = pool.get(clazz.getName());

    if (definalize) {
      final CtClass parentClass = ctClass.getDeclaringClass();

      if (parentClass != null) { // must process the parent for inner classes but leave it unfrozen
        definalizeClass(parentClass);
      }
      definalizeClass(ctClass);
    }
    ctClass.stopPruning(true);
    // use the same protection domain as the original class loaded from our parent
    return ctClass.toClass(this, clazz.getProtectionDomain());
  }

  private void definalizeClass(CtClass ctClass) {
    ctClass.defrost();
    final int modifiers = ctClass.getModifiers();

    if (Modifier.isFinal(modifiers)) {
      ctClass.setModifiers(Modifier.clear(modifiers, Modifier.FINAL));
    }
    Stream.of(ctClass.getDeclaredMethods()).forEach(this::definalizeMethod);
  }

  private void definalizeMethod(CtMethod ctMethod) {
    final int modifiers = ctMethod.getModifiers();

    if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers)) {
      ctMethod.setModifiers(Modifier.clear(modifiers, Modifier.FINAL));
    }
  }
}
