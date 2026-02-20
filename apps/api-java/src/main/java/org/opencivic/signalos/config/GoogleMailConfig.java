package org.opencivic.signalos.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleMailConfig {

    @Value("${application.google.credentials-json:}")
    private String credentialsJson;

    @Bean
    public Gmail gmailService() throws IOException, GeneralSecurityException {
        if (credentialsJson == null || credentialsJson.isBlank()) {
            return null; // Fallback to dummy or log warning if not provided
        }

        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(credentialsJson.getBytes()))
                .createScoped(Collections.singleton(GmailScopes.GMAIL_SEND));

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Open Civic Signal OS")
                .build();
    }
}
