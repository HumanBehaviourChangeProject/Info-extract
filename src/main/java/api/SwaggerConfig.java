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
          .build();                                           
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("HBCP Information Extraction API")
            .description("HBCP information extraction APIs allow users to extract information from " +
"behaviour change literature according to Human Behaviour Change Ontology (HBCO). \n" +
"Using this page to test APIs directly. For each API, please use the browse button to upload a pdf file (i.e., a human behaviour change clinical study article) from which you want to extract information and click the \"try it out\" button.\n"
+ "# Available APIs \n "
+ "## (1) Extract BCTs \n"
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
"## (2) Extract clinical study population characteristics: \n" +
"Currently four APIs are provided to extract the gender, maximum age,\n" +
"mean age, and minimum age information from a given research paper. \n\n"
//+ "## [HBCP JavaDoc](../apidocs/index.html)"
            )
            .contact(new Contact("HBCP team, IBM Research, Dublin", "https://researcher.watson.ibm.com/researcher/view_group.php?id=8205", "debasis.ganguly1@ie.ibm.com"))
            .license("Apache License Version 2.0")
            .licenseUrl("https://github.ibm.com/Dublin-Research-Lab/hbcpIE")
            .version("0.1")
            .build();
    }    
    
}