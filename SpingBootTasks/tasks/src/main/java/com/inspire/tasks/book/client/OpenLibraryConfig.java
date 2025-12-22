package com.inspire.tasks.book.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class OpenLibraryConfig {

    @Bean
    OpenLibraryService catFactService(){
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openlibrary.org")
                .build();


        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();

        return factory.createClient(OpenLibraryService.class);
    }
}
