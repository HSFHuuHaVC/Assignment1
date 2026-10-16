/**
 * Transactional implementations of business services.
 *
 * <p>Each implementation uses constructor injection, {@code @Service} and
 * {@code @Transactional(readOnly = true)} at class level. Methods that change
 * data override this with {@code @Transactional}. Business rules belong in
 * these implementations; controllers depend on service interfaces rather
 * than repositories. Implementations are supplied by their feature issues.
 */
package com.fu.cinema.service.impl;
