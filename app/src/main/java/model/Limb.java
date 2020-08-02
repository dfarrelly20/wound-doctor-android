package model;

public class Limb {

    private String limbId;
    private String limbName;
    private String imageUrl;

    public Limb(){}

    public Limb(String limbId, String limbName, String imageUrl) {
        this.limbId = limbId;
        this.limbName = limbName;
        this.imageUrl = imageUrl;
    }

    public String getLimbId() {
        return limbId;
    }

    public void setLimbId(String limbId) {
        this.limbId = limbId;
    }

    public String getLimbName() {
        return limbName;
    }

    public void setLimbName(String limbName) {
        this.limbName = limbName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
