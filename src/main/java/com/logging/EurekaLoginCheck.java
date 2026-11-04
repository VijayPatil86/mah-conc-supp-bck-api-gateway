package com.logging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EurekaLoginCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EurekaLoginCheck.class);

    private final String zone;

    public EurekaLoginCheck(@Value("${eureka.client.service-url.defaultZone}") String zone) {
        this.zone = zone;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // defaultZone may list several servers separated by commas; test the first
            URI uri = URI.create(zone.split(",")[0].trim());
            String userInfo = uri.getUserInfo();                  // "user:password", or null
            URI target = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath().replaceAll("/?$", "/") + "apps", null, null);

            HttpRequest.Builder request = HttpRequest.newBuilder(target)
                    .timeout(Duration.ofSeconds(2))
                    .GET();
            if (userInfo != null) {
                String token = Base64.getEncoder()
                        .encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));
                request.header("Authorization", "Basic " + token);
            }

            int status = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
                    .send(request.build(), HttpResponse.BodyHandlers.discarding())
                    .statusCode();

            if (status == 401) {
                log.error("EUREKA LOGIN REJECTED (401). This service will not register. "
                        + "Check eureka.password against the discovery server.");
            } else if (status >= 200 && status < 300) {
                log.info("Eureka login accepted");
            } else {
                log.warn("Eureka returned status {}", status);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Could not reach the discovery server: {}", e.getClass().getSimpleName());
        }
    }
}