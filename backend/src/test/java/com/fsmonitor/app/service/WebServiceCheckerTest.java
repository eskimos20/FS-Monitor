package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class WebServiceCheckerTest {

    private Service serviceOf(ServiceType type, int port) {
        Service service = new Service();
        service.setName("test");
        service.setType(type);
        service.setHost("example.test");
        service.setPort(port);
        return service;
    }

    private MockRestServiceServer mockServerFor(WebServiceChecker checker) {
        RestTemplate restTemplate = getRestTemplate(checker);
        return MockRestServiceServer.bindTo(restTemplate).build();
    }

    private RestTemplate getRestTemplate(WebServiceChecker checker) {
        try {
            var field = WebServiceChecker.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            return (RestTemplate) field.get(checker);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void httpsServiceUsesHttpsScheme() {
        WebServiceChecker checker = new WebServiceChecker(new RestTemplateBuilder());
        MockRestServiceServer server = mockServerFor(checker);
        server.expect(requestTo("https://example.test:443/"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        assertTrue(checker.check(serviceOf(ServiceType.HTTPS, 443)));
        server.verify();
    }

    @Test
    void httpServiceUsesHttpScheme() {
        WebServiceChecker checker = new WebServiceChecker(new RestTemplateBuilder());
        MockRestServiceServer server = mockServerFor(checker);
        server.expect(requestTo("http://example.test:8080/"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        assertTrue(checker.check(serviceOf(ServiceType.WEB, 8080)));
        server.verify();
    }

    @Test
    void clientErrorResponsesStillMeanOnline() {
        for (HttpStatus status : new HttpStatus[]{
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND, HttpStatus.MOVED_PERMANENTLY}) {
            WebServiceChecker checker = new WebServiceChecker(new RestTemplateBuilder());
            MockRestServiceServer server = mockServerFor(checker);
            server.expect(requestTo("http://example.test:80/"))
                    .andRespond(withStatus(status));

            assertTrue(checker.check(serviceOf(ServiceType.WEB, 80)),
                    status + " should count as ONLINE - the service answered");
            server.verify();
        }
    }

    @Test
    void serverErrorMeansOffline() {
        WebServiceChecker checker = new WebServiceChecker(new RestTemplateBuilder());
        MockRestServiceServer server = mockServerFor(checker);
        server.expect(requestTo("http://example.test:80/"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertFalse(checker.check(serviceOf(ServiceType.WEB, 80)));
        server.verify();
    }
}
