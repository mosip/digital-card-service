package io.mosip.digitalcard.entity;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * The DigitalCardEntity.
 *
 * @author Dhanendra
 */
@Data
@Entity
@Table(name = "digitalcard_transaction", schema = "digitalcard")
public class DigitalCardTransactionEntity {

    /** The request id. */
    @Id
    @Column(name = "rid", nullable = false)
    private String rid;


    @Column(name = "credential_id")
    private String credentialId;

    /** The uinSaltedHash. */
    @Column(name = "uin_salted_hash")
    private String uinSaltedHash;

    /** The status code. */
    @Column(name = "status_code", nullable = false)
    private String statusCode;

    /** The status code. */
    @Column(name = "status_comment")
    private String statusComment;

    @Column(name = "datashareurl")
    private String dataShareUrl;

    @Column(name = "issuancedate")
    private LocalDateTime issuanceDate;

    /** The created by. */
    @Column(name = "cr_by")
    private String createdBy;

    /** The create date time. */
    @Column(name = "cr_dtimes", updatable = false)

    private LocalDateTime createDateTime;

    /** The updated by. */
    @Column(name = "upd_by")
    private String updatedBy;

    /** The update date time. */
    @Column(name = "upd_dtimes")
    private LocalDateTime updateDateTime;

    /** The is deleted. */
    @Column(name = "is_deleted")
    private Boolean isDeleted;

    /** The deleted date time. */
    @Column(name = "del_dtimes")
    private LocalDateTime deletedDateTime;

    /**
     * Gets the request id.
     *
     * @return the request id
     */
    public String getrid() {
        return rid;
    }

    /**
     * Sets the request id.
     *
     * @param rid the new request id
     */
    public void setrid(String rid) {
        this.rid = rid;
    }


    /**
     * Gets the status code.
     *
     * @return the status code
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the status code.
     *
     * @param statusCode the new status code
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the created by.
     *
     * @return the created by
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the created by.
     *
     * @param createdBy the new created by
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the creates the date time.
     *
     * @return the creates the date time
     */
    public LocalDateTime getCreateDateTime() {
        return createDateTime;
    }

    /**
     * Sets the creates the date time.
     *
     * @param createDateTime the new creates the date time
     */
    public void setCreateDateTime(LocalDateTime createDateTime) {
        this.createDateTime = createDateTime;
    }

    /**
     * Gets the updated by.
     *
     * @return the updated by
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the updated by.
     *
     * @param updatedBy the new updated by
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Gets the update date time.
     *
     * @return the update date time
     */
    public LocalDateTime getUpdateDateTime() {
        return updateDateTime;
    }

    /**
     * Sets the update date time.
     *
     * @param updateDateTime the new update date time
     */
    public void setUpdateDateTime(LocalDateTime updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    /**
     * Gets the checks if is deleted.
     *
     * @return the checks if is deleted
     */
    public Boolean getIsDeleted() {
        return isDeleted;
    }

    /**
     * Sets the checks if is deleted.
     *
     * @param isDeleted the new checks if is deleted
     */
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getUinSaltedHash() {
        return uinSaltedHash;
    }

    public void setUinSaltedHash(String uinSaltedHash) {
        this.uinSaltedHash = uinSaltedHash;
    }

    /**
     * Gets the deleted date time.
     *
     * @return the deleted date time
     */
    public LocalDateTime getDeletedDateTime() {
        return deletedDateTime;
    }

    /**
     * Sets the deleted date time.
     *
     * @param deletedDateTime the new deleted date time
     */
    public void setDeletedDateTime(LocalDateTime deletedDateTime) {
        this.deletedDateTime = deletedDateTime;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getDataShareUrl() {
        return dataShareUrl;
    }

    public void setDataShareUrl(String dataShareUrl) {
        this.dataShareUrl = dataShareUrl;
    }

    public LocalDateTime getIssuanceDate() {
        return issuanceDate;
    }

    public void setIssuanceDate(LocalDateTime issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    public String getStatusComment() {
        return statusComment;
    }

    public void setStatusComment(String statusComment) {
        this.statusComment = statusComment;
    }
}
