package org.opencivic.signalos.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleMailConfig {

    private static final String APPLICATION_NAME = "Open Civic Signal OS";
    private static final String JSON_PATH = "C:\Users\jdsal\Downloads
8n-workflows-468303-f3b3854a2b9c.json";

    @Bean
    public Gmail gmailService() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(JSON_PATH))
                .createScoped(Collections.singleton(GmailScopes.GMAIL_SEND));
        
        // If it's a service account, we might need to impersonate a user if sending as "me"
        // But for testing purposes, we'll try basic service account credentials first
        
        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
