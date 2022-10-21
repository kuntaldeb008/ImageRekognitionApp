package com.example.amplifyimagerekognition;

public class LabelModel {

    String labelName;
    String confidence;

    public LabelModel() {
        super();
        /* TODO Auto-generated constructor stub */
    }


    public LabelModel(String labelName) {
        this.labelName = labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getLabelName() {
        return labelName;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "LabelModel{" +
                "labelName='" + labelName + '\'' +
                ", confidence='" + confidence + '\'' +
                '}';
    }
}
