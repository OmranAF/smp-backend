package com.smp.prescription;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smp.prescription.dto.PrescriptionTicketDetailsDto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionTicketController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/tickets/{ticketId}")
    public PrescriptionTicketDetailsDto getTicketDetails(
            @PathVariable UUID ticketId,
            @RequestParam String token) {
        return prescriptionService.getTicketDetails(ticketId, token);
    }

    @GetMapping(value = "/tickets/{ticketId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getTicketQrCode(
            @PathVariable UUID ticketId,
            @RequestParam String token) {
        PrescriptionDao prescription = prescriptionService.findTicketPrescription(ticketId, token);
        String payloadUrl = prescriptionService.buildQrPayloadUrl(prescription);

        try {
            byte[] imageBytes = generateQrPng(payloadUrl, 320, 320);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (WriterException | IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate QR code", ex);
        }
    }

    private byte[] generateQrPng(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", output);
        return output.toByteArray();
    }
}
