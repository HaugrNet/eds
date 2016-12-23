package io.javadog.cws.model;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class EntityManagerSetup {

    private static final String persistenceName = "io.javadog.cws.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(persistenceName);

    private EntityManagerSetup() {
        // Private Constructor, this is a Utility Class
    }

    public static EntityManager createEntityManagerInstance() {
        return FACTORY.createEntityManager();
    }
}
