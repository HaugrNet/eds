/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.junit.jupiter.api.Test;

/**
 * <p>A request with a passphrase reads its account in a transaction of its
 * own, and checks the passphrase (PBKDF2) before its own transaction reads
 * anything, so it holds no connection of the database meanwhile (ticket
 * 28). The Bean's own transactions are stood in for by an entity manager of
 * their own.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class PassphraseBeforeReadTest extends DatabaseSetup {

    @Test
    void testThePassphraseIsCheckedBeforeTheRequestReadsItsAccount() {
        final Settings limited = limitedSettings();
        final List<Boolean> accountReadByTheRequest = new ArrayList<>();
        try (EntityManager own = entityManager.getEntityManagerFactory().createEntityManager()) {
            final AuthLimitBean probe = new AuthLimitBean(own, limited) {
                @Override
                public boolean begin(final long memberId) {
                    // Called right before the passphrase is checked.
                    accountReadByTheRequest.add(holdsAMember());
                    return true;
                }

                @Override
                public void succeeded(final long memberId) {
                    // Nothing is counted here.
                }
            };
            final ProcessMemberResponse response = login(probe, limited, MEMBER_1);
            assertTrue(response.isOk(), response.getReturnMessage());
        }
        assertEquals(List.of(false), accountReadByTheRequest);
    }

    @Test
    void testAnAccountChangedMeanwhileIsCheckedAgain() {
        // The request's transaction finds the account changed since it was
        // read in a transaction of its own, as after a new passphrase: the
        // passphrase is checked again, against the account as it is now.
        final Settings limited = limitedSettings();
        final List<Long> checked = new ArrayList<>();
        try (EntityManager own = entityManager.getEntityManagerFactory().createEntityManager()) {
            final AuthLimitBean probe = new AuthLimitBean(own, limited) {
                @Override
                public boolean begin(final long memberId) {
                    checked.add(memberId);
                    return true;
                }

                @Override
                public void succeeded(final long memberId) {
                    // Nothing is counted here.
                }
            };
            final MemberEntity changed = dao.findMemberByName(MEMBER_1);
            changed.setPublicKey(changed.getPublicKey() + ' ');
            login(probe, limited, MEMBER_1);
        }
        assertEquals(2, checked.size());
    }

    private Settings limitedSettings() {
        final Settings limited = newSettings();
        // With a limit, each check of a passphrase is counted first, which
        // shows when it happens.
        limited.set(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS.getKey(), "5");
        return limited;
    }

    private ProcessMemberResponse login(final AuthLimitBean probe, final Settings limited, final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        return new ManagementBean(entityManager, schemaManager, limited, null, null, probe).processMember(request);
    }

    private boolean holdsAMember() {
        return entityManager.unwrap(Session.class).getStatistics().getEntityKeys().stream()
                .anyMatch(key -> (key instanceof EntityKey entity) && MemberEntity.class.getName().equals(entity.getEntityName()));
    }
}
