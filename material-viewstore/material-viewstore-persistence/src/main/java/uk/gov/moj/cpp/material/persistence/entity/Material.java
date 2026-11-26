package uk.gov.moj.cpp.material.persistence.entity;

import static java.util.Objects.hash;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "material")
@Access(value = AccessType.FIELD)
public class Material {

    @Id
    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "date_material_added")
    private ZonedDateTime dateMaterialAdded;

    @Column(name = "filename")
    private String filename;

    @Column(name = "alfresco_id")
    private String alfrescoId;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "external_link")
    private String externalLink;


    public Material() {
        //default constructor
    }

    public Material(final UUID materialId, final String alfrescoId, final String filename, final String mimeType, final ZonedDateTime dateMaterialAdded, final String externalLink) {
        this.materialId = materialId;
        this.dateMaterialAdded = dateMaterialAdded;
        this.filename = filename;
        this.mimeType = mimeType;
        this.alfrescoId = alfrescoId;
        this.externalLink = externalLink;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public ZonedDateTime getDateMaterialAdded() {
        return dateMaterialAdded;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getAlfrescoId() {
        return alfrescoId;
    }

    public String getExternalLink() {
        return externalLink;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Material material = (Material) o;
        return Objects.equals(getMaterialId(), material.getMaterialId()) &&
                Objects.equals(getDateMaterialAdded(), material.getDateMaterialAdded()) &&
                Objects.equals(getFilename(), material.getFilename()) &&
                Objects.equals(getMimeType(), material.getMimeType()) &&
                Objects.equals(getExternalLink(), material.getExternalLink()) &&
                Objects.equals(getAlfrescoId(), material.getAlfrescoId());
    }

    @Override
    public int hashCode() {
        return hash(getMaterialId(), getDateMaterialAdded(), getFilename(), getMimeType(), getAlfrescoId(), getExternalLink());
    }
}