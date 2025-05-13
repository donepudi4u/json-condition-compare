
package com.example.comparator;

import java.util.List;

public class EventConfigurationDTO {
    private WebhookEventDetailsDTO eventDetails;
    private ConditionGroupConfigurationDTO conditionGroup;

    public WebhookEventDetailsDTO getEventDetails() { return eventDetails; }
    public void setEventDetails(WebhookEventDetailsDTO eventDetails) { this.eventDetails = eventDetails; }

    public ConditionGroupConfigurationDTO getConditionGroup() { return conditionGroup; }
    public void setConditionGroup(ConditionGroupConfigurationDTO conditionGroup) { this.conditionGroup = conditionGroup; }
}

class WebhookEventDetailsDTO {
    // Define fields if needed
}

class ConditionGroupConfigurationDTO {
    private String fieldName;
    private String validationCondition;
    private List<String> fieldValues;
    private ConditionGroupConfigurationDTO conditionGroup;

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getValidationCondition() { return validationCondition; }
    public void setValidationCondition(String validationCondition) { this.validationCondition = validationCondition; }

    public List<String> getFieldValues() { return fieldValues; }
    public void setFieldValues(List<String> fieldValues) { this.fieldValues = fieldValues; }

    public ConditionGroupConfigurationDTO getConditionGroup() { return conditionGroup; }
    public void setConditionGroup(ConditionGroupConfigurationDTO conditionGroup) { this.conditionGroup = conditionGroup; }
}
