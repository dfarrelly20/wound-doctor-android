package util;

import android.app.Application;

public class PatientManager extends Application {

    private String patientId;
    private String patientName;

    private static PatientManager instance;

    public static PatientManager getInstance(){
        if (instance == null)
            instance = new PatientManager();
        return instance;
    }

    public PatientManager(){}

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
}
