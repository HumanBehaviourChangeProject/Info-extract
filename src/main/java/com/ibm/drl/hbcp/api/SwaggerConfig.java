/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

import com.google.common.base.Predicates;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import springfox.documentation.service.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
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
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("HBCP APIs: Extractor API and Predictor API\n\n")
           
            .description("These APIs have been developed in the context of the HBCP project. Please refer to the project website for any detail: https://www.humanbehaviourchange.org/\n"
                    +"Use this page to test APIs directly. \n"
                    
                    + "# Extractor API\n"
                    + "HBCP information extraction APIs allow users to extract information from " 
                    + "behaviour change literature according to Human Behaviour Change Ontology (HBCO). \n"
                    
                    + "## Extract BCTs \n"
                    + "We provide two APIs (/extract/allbcts, /extract/allbcts/supervised) to extract all BCTs for a given research paper. Currently the following 10 BCTs are supported: \n" +
                        "* 1.1 Goal setting (behavior)\n" +
                        "* 1.2 Problem solving\n" +
                        "* 1.4 Action planning\n" +
                        "* 2.2 Feedback on behaviour\n" +
                        "* 2.3 Self-monitoring of behavior\n" +
                        "* 3.1 Social support (unspecified)\n" +
                        "* 5.1 Information about health consequences\n" +
                        "* 5.3 Information about social and environmental consequences\n" +
                                    "* 11.1 Pharmacological support\n" +
                        "* 11.2 Reduce negative emotions\n " +
                        "\n\n Users can also extract a specific BCT using /extract/bcts or /extract/bcts/supervised\n" +
                        "by specifying the targeting BCT code (e.g., 1.1 or 1.2). \n\n" +
                    
                        "## Extract clinical study population characteristics: \n" +
                        "Currently four APIs are provided to extract the gender, maximum age,\n" +
                        "mean age, and minimum age information from a given research paper.\n\n"+
                "# Predictor API\n"+
                    "HBCP Predictor API allows users to predict the most likely outcome of a study given "
                    + " some population characteristics and some BCTs. In the future, the user will be able to "
                    + "specify also experimental settings.\n "
                    + "The attributeInfo, allINputOption, expPptionEndpoint, genderOptionEndpoint, populationOptionEndopoint and interventionOptionEndpoint Apis"
                    + " allow the user to retrieve specific popualtion attributes or intervention attributes,"
                    + " that can be used for prediciton.\n"
                    + "PredictOutcome Api allows the user to specify some population value AND/OR intervention value and obtain the predicted outcome. "
                    + "\nThe following examples"
                    + "show the query format:\n"
                    + "* For Population query please use the format: C:attributeID:value. Eg. for Mean Age= 25.99 please  write  C:4507435:25\n" 
                    + "* For Invervention query please use the format: I:attributeID:value. Eg. for intevention Social support (unspecified) please write I:3675717:1.0\n"
                    + "* For multiple population or intervention queries please concatenate with & Eg. C:4507433:18&C:4507434:50")
                
                
            .contact(new Contact("HBCP team, IBM Research, Dublin", "https://researcher.watson.ibm.com/researcher/view_group.php?id=8205", "debasis.ganguly1@ie.ibm.com"))
            .license("Apache License Version 2.0")
            .licenseUrl("https://github.ibm.com/Dublin-Research-Lab/hbcpIE")
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