package util;

import android.app.Application;

/**
 * Class is used to create a single instance of a patient signed into the app. The ID and first
 * name of the patient are added to the instance.
 *
 * @author David Farrelly
 */
public class PatientManager extends Application {

    /**
     * The ID of the patient currently signed into the app
     */
    private String patientId;

    /**
     * The first name of the patient currently signed into the app
     */
    private String patientName;

    /**
     * A reference to the current instance of PatientManager
     */
    private static PatientManager instance;

    /**
     * Method first checks if no instance of PatientManager exists. If there is none,
     * an instance is created here
     * @return
     */
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
