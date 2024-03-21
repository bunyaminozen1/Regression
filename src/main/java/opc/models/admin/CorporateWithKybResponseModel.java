package opc.models.admin;

import java.util.List;

public class CorporateWithKybResponseModel {

    private CorporateResponseModel corporate;
    private CorporateKybResponseModel kyb;
    private List<UboResponseModel> ubo;
    private List<DirectorResponseModel> director;

    public CorporateResponseModel getCorporate() {
        return corporate;
    }

    public void setCorporate(CorporateResponseModel corporate) {
        this.corporate = corporate;
    }

    public CorporateKybResponseModel getKyb() {
        return kyb;
    }

    public void setKyb(CorporateKybResponseModel kyb) {
        this.kyb = kyb;
    }

    public List<UboResponseModel> getUbo() {
        return ubo;
    }

    public void setUbo(List<UboResponseModel> ubo) {
        this.ubo = ubo;
    }

    public List<DirectorResponseModel> getDirector() {
        return director;
    }

    public void setDirector(List<DirectorResponseModel> director) {
        this.director = director;
    }

}
