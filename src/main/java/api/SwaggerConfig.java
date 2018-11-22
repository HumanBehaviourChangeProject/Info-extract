/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import com.google.common.base.Predicates;
import springfox.documentation.service.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * This class configures the swagger interface for demonstrating the REST APIs.
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
          .build().useDefaultResponseMessages(false);
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("HBCP Information Extraction API")
            .description("HBCP information extraction APIs allow users to extract information from " +
"behaviour change literature according to Human Behaviour Change Ontology (HBCO). \n" +
"For more information on the implementation and on the dataset used for this API please see: https://github.com/HumanBehaviourChangeProject/Info-extract \n\n" +   
"##Supported BCTs:\n" +
"Currently the following 10 BCTs are supported: \n\n" +
"CODE BCT \n" +
"* 1.1 Goal setting (behavior)\n" +
"* 1.2 Problem solving\n" +
"* 1.4 Action planning\n" +
"* 2.2 Feedback on behaviour\n" +
"* 2.3 Self-monitoring of behavior\n" +
"* 3.1 Social support (unspecified)\n" +
"* 5.1 Information about health consequences\n" +
"* 5.3 Information about social and environmental consequences\n" +
"* 11.1 Pharmacological support\n" +
"* 11.2 Reduce negative emotions\n\n " +
"#Quickstart:\n\n"+ 
"* Click on doc-extract-info\n" +
"* Choose the API according to the entity you want to extract from your pdf:\n" +
"  * all BCTs present in the paper (allbcts)\n" +
"  * all BCTs present in the paper using a supervised method (allbcts/supervised)\n" +
"  * detect the presence of the bct specified in the parameter \"code\" (bct)\n" +
"  * detect the presence of the bct specified in the parameter \"code\" using a supervised method (bct/supervised)\n" +
"  * the gender of participants (gender)\n" +
"  * the minimum age of participants (minage)\n" +
"  * the max of participants (maxage)\n" +
"  * the mean of participants (meanage)\n" +
"  * Upload the pdf with the : \"choose file\" button\n" +
"* Click \"Try it out!\"\n\n\n"
//+ "## [HBCP JavaDoc](../apidocs/index.html)"
             )
            .contact(new Contact("HBCP team", "https://www.humanbehaviourchange.org/", ""))
            .license("Apache License Version 2.0")
            .licenseUrl("https://github.com/HumanBehaviourChangeProject/Info-extract")
            .version("0.1")
            .build();
    }    
    
}