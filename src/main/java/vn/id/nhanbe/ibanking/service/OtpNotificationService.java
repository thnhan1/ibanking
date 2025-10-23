package vn.id.nhanbe.ibanking.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OtpNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@ibanking.local}")
    private String defaultFrom;

    public void sendOtp(String recipientEmail, String otpCode, String referenceCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultFrom);
        message.setTo(recipientEmail);
        message.setSubject("Your OTP code");
        message.setText(buildBody(otpCode, referenceCode));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send OTP email to {}", recipientEmail, ex);
            throw ex;
        }
    }

    private String buildBody(String otpCode, String referenceCode) {
        return """
                Xin chào,

                Mã OTP của bạn là %s. Mã này sẽ hết hạn sau vài phút.
                Giao dịch tham chiếu: %s

                Nếu bạn không yêu cầu giao dịch này, vui lòng liên hệ bộ phận hỗ trợ ngay lập tức.

                Trân trọng,
                iBanking Support
                """.formatted(otpCode, referenceCode);
    }
}
