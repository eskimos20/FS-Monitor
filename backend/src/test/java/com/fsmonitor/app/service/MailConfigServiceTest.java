package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.MailConfig;
import com.fsmonitor.app.repository.MailConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MailConfigServiceTest {

    @Autowired
    MailConfigService mailConfigService;

    @Autowired
    MailConfigRepository mailConfigRepository;

    private MailConfig config(String host, Integer port, String from, String to) {
        MailConfig c = new MailConfig();
        c.setHost(host);
        c.setPort(port);
        c.setFromEmail(from);
        c.setToEmail(to);
        return c;
    }

    @Test
    void secondSaveWithIdUpdatesInPlace() {
        MailConfig first = mailConfigService.saveMailConfig(
                config("smtp.one.com", 25, "a@x.com", "b@x.com"));
        assertNotNull(first.getId());

        // Simulate PUT /{id}: the controller sets the id on the incoming entity.
        // Regression: deleteAll()+save() on a detached entity with an assigned
        // IDENTITY id threw PersistentObjectException and nothing was stored.
        MailConfig update = config("smtp.two.com", 587, "c@x.com", "d@x.com");
        update.setId(first.getId());
        MailConfig second = mailConfigService.saveMailConfig(update);

        assertEquals(first.getId(), second.getId(), "existing row must be updated, not replaced");
        assertEquals("smtp.two.com", second.getHost());
        assertEquals(587, second.getPort());
        assertEquals("c@x.com", second.getFromEmail());
        assertEquals("d@x.com", second.getToEmail());
        assertTrue(mailConfigRepository.count() >= 1);
        assertEquals(second.getId(),
                mailConfigRepository.findFirstByOrderByIdDesc().orElseThrow().getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void blankPasswordPreservesStoredPassword() {
        MailConfig first = config("smtp.pw.com", 465, "a@x.com", "b@x.com");
        first.setPassword("s3cret");
        MailConfig saved = mailConfigService.saveMailConfig(first);
        assertTrue(saved.isPasswordSet());

        MailConfig update = config("smtp.pw2.com", 465, "a@x.com", "b@x.com");
        update.setId(saved.getId());
        update.setPassword("");
        MailConfig result = mailConfigService.saveMailConfig(update);

        assertTrue(result.isPasswordSet(), "blank incoming password must keep the stored one");
        assertEquals("smtp.pw2.com", result.getHost());
    }
}
