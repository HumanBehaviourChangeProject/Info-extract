/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

import com.google.common.base.Predicates;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 * @author dganguly
 */

@Configuration
@EnableSwagger2
@EnableAutoConfiguration(exclude={MultipartAutoConfiguration.class})
public class SwaggerConfig {                                    
    @Bean
    public Docket api() { 
        return new Docket(DocumentationType.SWAGGER_2)  
          .apiInfo(apiInfo())
          .select()                                  
          .apis(RequestHandlerSelectors.any())              
          //.paths(PathSelectors.any())
          .paths(Predicates.not(PathSelectors.regex("/error.*")))
          .build();                                           
    }


    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver
                = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(1000000000);
        return multipartResolver;
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("HBCP APIs\n\n")
           // description in Markdown
            .description("These APIs have been developed in the context of the [HBCP project](https://www.humanbehaviourchange.org/). Please refer to the project website for any details.\n"
                    +"This page can be used to test the APIs directly. \n"

                    + "# Extractor API\n"
                    + "The HBCP information extraction API allows users to extract information from "
                    + "behaviour change literature according to the Behaviour Change Intervention Ontology (BCIO).\n"
                    + "The two more useful REST endpoints extract all entities from a single PDF ([/api/extract/all](#!/extractor45controller/extractAllUsingPOST)) or multiple PDFs ([/api/extract/all/multi](#!/extractor45controller/extractAllMultiUsingPOST)).\n\n" +
                "# Predictor API\n"+
                    "The HBCP prediction API allows users to predict an outcome value (e.g., percentage of participants to stop smoking), given certain study conditions,"
                    + " i.e., population characteristics and/or interventions/BCTs.\n "
                    + "The REST endpoint for prediction ([/api/predict/outcome](#!/predictor45controller/predictOutcomeEndpointUsingGET)) requires population and intervention queries in the following format:\n"
                    + "* For Population query please use the format: `C:attributeID:value`, e.g., for Mean Age= 25.99 please  write `C:4507435:25`\n"
                    + "* For Intervention query please use the format: `I:attributeID:value`, e.g., for 'Social support (unspecified)' please write `I:3675717:1.0`\n"
                    + "* For multiple population or intervention queries please concatenate with '&', e.g., \"C:4507433:18&C:4507434:50\"")
                
                
            .contact(new Contact("HBCP team, IBM Research, Dublin", "https://researcher.watson.ibm.com/researcher/view_group.php?id=8205", "debasis.ganguly1@ie.ibm.com"))
            .license("Apache License Version 2.0")
            .licenseUrl("https://github.com/HumanBehaviourChangeProject/Info-extract/blob/master/LICENSE")
            .version("0.1")
            .build();
    }
    
     

    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Arrays.asList(new MediaType("application", "json", Charset.forName("UTF-8"))));
        return converter;
    }

}