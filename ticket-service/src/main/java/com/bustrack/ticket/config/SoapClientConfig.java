package com.bustrack.ticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

@Configuration
public class SoapClientConfig {

    @Bean
    public Jaxb2Marshaller paymentMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.bustrack.ticket.soap");   // classes generated from payment.xsd
        return marshaller;
    }

    @Bean
    public WebServiceTemplate paymentWebServiceTemplate(
            Jaxb2Marshaller paymentMarshaller,
            @Value("${bustrack.payment.uri}") String uri,
            @Value("${bustrack.payment.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${bustrack.payment.read-timeout-ms}") long readTimeoutMs) {

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(connectTimeoutMs));
        sender.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(paymentMarshaller);
        template.setUnmarshaller(paymentMarshaller);
        template.setDefaultUri(uri);
        template.setMessageSender(sender);
        return template;
    }
}
