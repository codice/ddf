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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Associate sets of emails with an ID. This implementation will preserve any other properties
 * stored under the ID.
 */
public class SubscriptionsPersistentStoreImpl implements SubscriptionsPersistentStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsPersistentStore.class);

  private static final String EMAIL_PROPERTY = "emails";

  private static final String ID = "id";

  private static final Lock LOCK = new ReentrantLock();

  private final PersistentStore persistentStore;

  /** @param persistentStore must be non-null */
  public SubscriptionsPersistentStoreImpl(PersistentStore persistentStore) {
    notNull(persistentStore, "persistentStore must be non-null");
    this.persistentStore = persistentStore;
  }

  private List<Map<String, Object>> query(String q) throws PersistenceException {
    LOCK.lock();
    try {
      List<Map<String, Object>> results =
          persistentStore.get(SubscriptionsPersistentStore.SUBSCRIPTIONS_TYPE, q);
      assert results.size() <= 1;
      return results;
    } finally {
      LOCK.unlock();
    }
  }

  private List<Map<String, Object>> get(String id) throws PersistenceException {
    return query(queryId(id));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void addEmails(String id, Set<String> emails) {
    notBlank(id, "id must be non-blank");
    notNull(emails, "emails must be non-null");
    emails.forEach(email -> notBlank(email, "emails in set must be non-blank"));

    LOCK.lock();
    try {
      List<Map<String, Object>> results = get(id);

      if (!results.isEmpty()) {
        PersistentItem item = convert(results.get(0));

        if (item.containsKey(EMAIL_PROPERTY + PersistentItem.TEXT_SUFFIX)) {
          Set<String> newValue = new HashSet<>(emails);
          Object value = item.get(EMAIL_PROPERTY + PersistentItem.TEXT_SUFFIX);
          if (value instanceof String) {
            newValue.add((String) value);
          } else if (value instanceof Set) {
            ((Set) value)
                .stream()
                .filter(String.class::isInstance)
                .forEach(obj -> newValue.add((String) obj));
          }
          item.addProperty(EMAIL_PROPERTY, newValue);
        } else {
          item.addProperty(EMAIL_PROPERTY, emails);
        }
        persistentStore.add(SubscriptionsPersistentStore.SUBSCRIPTIONS_TYPE, item);
      } else {
        PersistentItem persistentItem = new PersistentItem();
        persistentItem.addIdProperty(id);
        persistentItem.addProperty(EMAIL_PROPERTY, emails);
        persistentStore.add(SubscriptionsPersistentStore.SUBSCRIPTIONS_TYPE, persistentItem);
      }

    } catch (PersistenceException e) {
      LOGGER.warn("unable to add emails to workspace: workspaceId={} emails={}", id, emails, e);
    } finally {
      LOCK.unlock();
    }
  }

  private String queryEmail(String email) {
    return "\"" + EMAIL_PROPERTY + "\"=" + quote(email);
  }

  private String queryId(String id) {
    return quote(ID) + "=" + quote(id);
  }

  private PersistentItem convert(Map<String, Object> map) {
    PersistentItem item = new PersistentItem();

    item.putAll(map);

    return item;
  }

  @SuppressWarnings("unchecked")
  private PersistentItem strip(PersistentItem item, Set<String> emails) {
    Object itemValue = item.get(EMAIL_PROPERTY + PersistentItem.TEXT_SUFFIX);
    Optional.ofNullable(itemValue)
        .ifPresent(
            value -> {
              if (value instanceof String) {
                String currentEmail = (String) value;
                if (emails.contains(currentEmail)) {
                  item.remove(EMAIL_PROPERTY + PersistentItem.TEXT_SUFFIX);
                }
              } else if (value instanceof Set) {
                Set<Object> currentEmails = new HashSet<>((Set) value);
                currentEmails.removeAll(emails);
                if (currentEmails.isEmpty()) {
                  item.remove(EMAIL_PROPERTY + PersistentItem.TEXT_SUFFIX);
                } else {
                  item.put(EMAIL_PROPERTY + PersistentItem.TEXT_SUFFIX, currentEmails);
                }
              }
            });

    return item;
  }

  private void add(String id, PersistentItem item) {
    LOCK.lock();
    try {
      persistentStore.add(SubscriptionsPersistentStore.SUBSCRIPTIONS_TYPE, item);
    } catch (PersistenceException e) {
      LOGGER.warn(
          "unable to add PersistentItem to the PersistentStore: id={} item={}", id, item, e);

    } finally {
      LOCK.unlock();
    }
  }

  @Override
  public void removeEmails(String id, Set<String> emails) {
    notBlank(id, "id must be non-blank");

    LOCK.lock();
    try {
      List<Map<String, Object>> results = get(id);

      results
          .stream()
          .map(this::convert)
          .map(item -> strip(item, emails))
          .forEach(item -> add(id, item));

    } catch (PersistenceException e) {
      LOGGER.warn("unable to delete emails from workspace: id={}", id, e);
    } finally {
      LOCK.unlock();
    }
  }

  private Set<String> merge(Set<String> set1, Set<String> set2) {
    return Stream.of(set1, set2).flatMap(Collection::stream).collect(Collectors.toSet());
  }

  private String quote(String value) {
    return "'" + value + "'";
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getSubscriptions(String email) {
    notBlank(email, "email must be non-blank");

    LOCK.lock();
    try {

      List<Map<String, Object>> results = query(queryEmail(email));

      List<Object> mapValues =
          results
              .stream()
              .map(PersistentItem::stripSuffixes)
              .filter(result -> result.containsKey(ID))
              .map(result -> result.get(ID))
              .collect(Collectors.toList());

      Set<String> emailsFromSet =
          streamToStrings(
              mapValues
                  .stream()
                  .filter(Set.class::isInstance)
                  .map(Set.class::cast)
                  .flatMap(Set::stream));

      Set<String> emailsFromString = streamToStrings(mapValues.stream());

      return merge(emailsFromSet, emailsFromString);
    } catch (PersistenceException e) {
      LOGGER.warn("unable to get workspace ids: email={}", email, e);
    } finally {
      LOCK.unlock();
    }

    return Collections.emptySet();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getEmails(String id) {
    notBlank(id, "id must be non-blank");

    LOCK.lock();
    try {

      List<Map<String, Object>> results = get(id);

      List<Object> mapValues =
          results
              .stream()
              .map(PersistentItem::stripSuffixes)
              .filter(result -> result.containsKey(EMAIL_PROPERTY))
              .map(result -> result.get(EMAIL_PROPERTY))
              .collect(Collectors.toList());

      Set<String> emailsFromSet =
          streamToStrings(
              mapValues
                  .stream()
                  .filter(Set.class::isInstance)
                  .map(Set.class::cast)
                  .flatMap(Set::stream));

      Set<String> emailsFromString = streamToStrings(mapValues.stream());

      return merge(emailsFromSet, emailsFromString);
    } catch (PersistenceException e) {
      LOGGER.warn("unable to get workspace emails: id={}", id, e);
    } finally {
      LOCK.unlock();
    }

    return Collections.emptySet();
  }

  /**
   * Convert a stream of objects into a set of strings.
   *
   * @param stream stream of objects
   * @return set of strings
   */
  private Set<String> streamToStrings(Stream<Object> stream) {
    return stream
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public final void addEmail(String id, String email) {
    notBlank(id, "id must be non-blank");
    notBlank(email, "email must be non-blank");

    addEmails(id, Collections.singleton(email));
  }

  @Override
  public final void removeAllEmails(String id) {
    removeEmails(id, getEmails(id));
  }

  @Override
  public final void removeEmail(String id, String email) {
    notBlank(id, "id must be non-blank");
    notBlank(email, "email must be non-blank");

    removeEmails(id, Collections.singleton(email));
  }
}
