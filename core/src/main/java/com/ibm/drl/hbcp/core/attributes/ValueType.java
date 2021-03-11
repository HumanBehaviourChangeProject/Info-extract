package com.ibm.drl.hbcp.core.attributes;

import com.beust.jcommander.internal.Lists;

import java.util.List;

/**
 * This class just keeps track of the value types for prioritized entities (rank 1,2,3)
 *
 */
public class ValueType {
    public static final List<String> PRESENCE_TYPE = Lists.newArrayList(
            "1.1.Goal setting (behavior)",
            "1.2 Problem solving ",
            "1.4 Action planning",
            "2.2 Feedback on behaviour ",
            "2.3 Self-monitoring of behavior",
            "3.1 Social support (unspecified)",
            "5.1 Information about health consequences",
            "5.3 Information about social and environmental consequences",
            "11.1 Pharmacological support ",
            "11.2 Reduce negative emotions ",
            "4.1 Instruction on how to perform the behavior",
            "4.5. Advise to change behavior",
            "Aggregate patient role",
            "Hospital facility",
            "Doctor-led primary care facility",
            "Smoking",
            "Self report",
            "Odds Ratio",
            "Face to face",
            "Distance",
            "Patch",
            "Individual",
            "Group-based",
            "Interventionist not otherwise specified",
            //rank3
            "Cognitive Behavioural Therapy",
            "Mindfulness",
            "Motivational Interviewing",
            "Brief advice",
            "Physical activity",
            "Pharmaceutical company funding",
            "Tobacco company funding",
            "Research grant funding",
            "No funding",
            "Pharmaceutical company competing interest",
            "Tobacco company competing interest",
            "Research grant competing interest",
            "No competing interest"
    );

    public static final List<String> VALUE_TYPE = Lists.newArrayList(
            "Arm name",
            "Outcome value",
            "Mean age",
            "Proportion identifying as female gender",
            "Mean number of times tobacco used",
            "Proportion identifying as male gender",
            "Lower-level geographical region",
            "Longest follow up",
            "Effect size p value",
            "Effect size estimate",
            "Proportion employed",
            "Proportion achieved university or college education",
            "Country of intervention",
            "Proportion in a legal marriage or union",
            "Mean number of years in education completed",
            "Aggregate health status type",
            "Site",
            "Individual-level allocated",
            "Individual-level analysed",
            "Expertise of Source",
            "Biochemical verification",
            "Website / Computer Program / App",
            "Printed material",
            "Digital content type",
            "Pill",
            "Somatic",
            "Health Professional",
            "Psychologist",
            "Researcher not otherwise specified",
            "Healthcare facility",
            //rank3
            "Dose",
            "Overall duration",
            "Number of contacts",
            "Contact frequency",
            "Contact duration",
            "Format",
            "Encountered intervention",
            "Completed intervention",
            "Sessions delivered"
    );
    public static final List<String> COMPLEX_TYPE = Lists.newArrayList(
            "Proportion identifying as belonging to a specific ethnic group",
            "Proportion belonging to specified family or household income category",
            "Proportion belonging to specified individual income category",
            "Aggregate relationship status",
            "Nicotine dependence",
            "Individual reasons for attrition"
    );


    public static boolean isPrioritizedEntity(Attribute attribute) {
        return isPresenceType(attribute) || isValueType(attribute) || isComplexType(attribute);
    }

    public static boolean isPresenceType(Attribute attribute) {
        return PRESENCE_TYPE.contains(attribute.getName());
    }

    public static boolean isValueType(Attribute attribute) {
        return VALUE_TYPE.contains(attribute.getName());
    }

    public static boolean isComplexType(Attribute attribute) {
        return COMPLEX_TYPE.contains(attribute.getName());
    }
}
