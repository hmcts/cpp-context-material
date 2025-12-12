package uk.gov.moj.cpp.material.domain;

import java.io.Serializable;
import java.util.Objects;

public class FileDetails implements Serializable {

    private static final long serialVersionUID = 34324343434324l;

    private String alfrescoAssetId;
    private String mimeType;
    private String fileName;
    private String externalLink;

    public FileDetails() {
        //default constructor
    }

    public FileDetails(final String externalLink, final String fileName) {
        this(null, null, externalLink, fileName);
    }

    public FileDetails(final String alfrescoAssetId, final String mimeType, final String fileName) {
        this(alfrescoAssetId, mimeType, null, fileName);
    }

    private FileDetails(final String alfrescoAssetId, final String mimeType, final String externalLink, final String fileName) {
        this.alfrescoAssetId = alfrescoAssetId;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.externalLink = externalLink;
    }

    public String getAlfrescoAssetId() {
        return alfrescoAssetId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
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
        final FileDetails that = (FileDetails) o;
        return Objects.equals(getAlfrescoAssetId(), that.getAlfrescoAssetId()) &&
                Objects.equals(getMimeType(), that.getMimeType()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getExternalLink(), that.getExternalLink());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAlfrescoAssetId(), getMimeType(), getFileName(), getExternalLink());
    }

    @Override
    public String toString() {
        return "FileDetails{" +
                "alfrescoAssetId='" + alfrescoAssetId + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", externalLink='" + externalLink + '\'' +
                '}';
    }
}
