package io.mosip.digitalcard.repositories;

import io.mosip.digitalcard.entity.DigitalCardTransactionEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The Interface DigitalCardTransactionRepository.
 *
 * @author Dhanendra
 */
@Repository
public interface DigitalCardTransactionRepository extends BaseRepository<DigitalCardTransactionEntity, String> {

    @Query("UPDATE DigitalCardTransactionEntity d SET d.statusCode = ?2, d.dataShareUrl = ?3, d.updateDateTime = ?4, d.updatedBy =?5 WHERE d.rid=?1")
    @Modifying
    @Transactional
    int updateTransactionDetails(String id, String status, String url, LocalDateTime updatedDT,String updateBy);

    @Query("UPDATE DigitalCardTransactionEntity d SET d.statusCode = ?2, d.statusComment = ?3, d.updateDateTime = ?4, d.updatedBy =?5 WHERE d.rid=?1")
    @Modifying
    @Transactional
    int updateErrorTransactionDetails(String id, String status, String statusComment, LocalDateTime updatedDT, String updateBy);

    @Query("FROM DigitalCardTransactionEntity WHERE rid=?1")
    DigitalCardTransactionEntity findByRID(String rid);
}