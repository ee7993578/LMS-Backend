package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.QR;
import com.learningJWT.LearningTemplate.Paylod.DTO.QRDTO;

public class QRMapper {
    public static QRDTO toDto(QR qr){
        return QRDTO.builder()
                .qrId(qr.getQrId())
                .libraryId(qr.getLibrary().getId())
                .qrCodeValue(qr.getQrCodeValue())
                .status(qr.getStatus())
                .build();
    }

    public static QR toEntity(QRDTO qr){
        return QR.builder()
                .qrId(qr.getQrId())
                .qrCodeValue(qr.getQrCodeValue())
                .status(qr.getStatus())
                .build();
    }
}
