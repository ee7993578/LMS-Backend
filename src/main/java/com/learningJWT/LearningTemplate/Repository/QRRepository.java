package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.QR;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QRRepository extends JpaRepository<QR,Long> {

    QR findByLibraryId(Long libraryId)throws Exception;
    Optional<QR> findByQrCodeValueAndLibraryIdAndStatus(String qrCodeValue, Long libraryId, Status status);}
