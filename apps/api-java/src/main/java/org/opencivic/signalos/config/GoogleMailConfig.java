package org.opencivic.signalos.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleMailConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleMailConfig.class);
    private static final String APPLICATION_NAME = "Open Civic Signal OS";

    @Bean
    public Gmail gmailService() throws IOException, GeneralSecurityException {
        try {
            // This will look for the file defined in GOOGLE_APPLICATION_CREDENTIALS environment variable
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singleton(GmailScopes.GMAIL_SEND));
            
            log.info("Google Credentials loaded successfully via standard environment flow.");

            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException e) {
            log.error("FAILED to load Google Credentials. Ensure GOOGLE_APPLICATION_CREDENTIALS is set to a valid JSON file path. Error: {}", e.getMessage());
            throw e;
        }
    }
}
