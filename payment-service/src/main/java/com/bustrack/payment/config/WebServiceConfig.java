package com.bustrack.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * SOAP endpoint at /ws (the MessageDispatcherServlet is registered by Spring Boot, path from spring.webservices.path).
 * WSDL is generated from the XSD and served at /ws/payment.wsdl.
 */
@EnableWs
@Configuration
public class WebServiceConfig {

    public static final String NAMESPACE = "http://bustrack.com/payment";

    @Bean
    public XsdSchema paymentSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/payment.xsd"));
    }

    @Bean(name = "payment")
    public DefaultWsdl11Definition paymentWsdl(XsdSchema paymentSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("PaymentPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace(NAMESPACE);
        wsdl.setSchema(paymentSchema);
        return wsdl;
    }
}
