package com.truper.bonusgenerator.service.email;

import com.truper.bonusgenerator.infrastructure.client.AiClient.AiAnalysisResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    @Value("${mail.to}")
    private String to;

    @Override
    public void sendCommitAnalysis(AiAnalysisResponse response) {
        validateMailConfig();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Analisis semanal de commits");
            helper.setText(buildHtmlBody(response), true);

            log.info("Enviando correo de analisis de commits. from={}, to={}", from, to);
            mailSender.send(message);
            log.info("Correo de analisis de commits enviado correctamente. to={}", to);
        } catch (MessagingException exception) {
            throw new EmailSendException(
                    "No fue posible construir el correo de analisis.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailSendException(
                    "No fue posible enviar el correo de analisis. Revisa MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM y MAIL_TO.",
                    exception
            );
        }
    }

    @Override
    public void sendTestEmail() {
        validateMailConfig();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Prueba de correo - Bonus Generator");
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; color: #1f2937;">
                      <h2 style="margin: 0 0 12px;">Bonus Generator</h2>
                      <p style="margin: 0;">Este es un correo de prueba enviado desde bonus-generator.</p>
                    </div>
                    """, true);

            log.info("Enviando correo de prueba. from={}, to={}", from, to);
            mailSender.send(message);
            log.info("Correo de prueba enviado correctamente. to={}", to);
        } catch (MessagingException exception) {
            throw new EmailSendException(
                    "No fue posible construir el correo de prueba.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailSendException(
                    "No fue posible enviar el correo de prueba. Revisa la configuracion SMTP.",
                    exception
            );
        }
    }

    private void validateMailConfig() {
        if (!StringUtils.hasText(from)) {
            throw new EmailSendException("MAIL_FROM no esta configurado.", null);
        }
        if (!StringUtils.hasText(to)) {
            throw new EmailSendException("MAIL_TO no esta configurado.", null);
        }
    }

    private String buildHtmlBody(AiAnalysisResponse response) {
        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0; padding:0; background:#f3f4f6; font-family:Arial, Helvetica, sans-serif; color:#111827;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f4f6; padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="width:640px; max-width:94%%; background:#ffffff; border:1px solid #e5e7eb;">
                          <tr>
                            <td style="background:#1f2937; padding:24px 28px;">
                              <h1 style="margin:0; color:#ffffff; font-size:22px; line-height:1.3;">Analisis semanal de commits</h1>
                              <p style="margin:8px 0 0; color:#d1d5db; font-size:14px;">Reporte generado automaticamente por Bonus Generator</p>
                            </td>
                          </tr>
                          %s
                          %s
                          %s
                          <tr>
                            <td style="padding:18px 28px 8px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse; background:#ffffff; border-top:1px solid #e5e7eb;">
                                <tr>
                                  <td colspan="2" style="padding:12px 0 8px; font-size:12px; font-weight:bold; color:#6b7280; text-transform:uppercase;">Metricas de IA</td>
                                </tr>
                                %s
                                %s
                                %s
                                %s
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 28px 24px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f9fafb; border:1px solid #e5e7eb;">
                                <tr>
                                  <td style="padding:16px 18px; color:#6b7280; font-size:12px; line-height:1.6;">
                                    <div style="color:#9ca3af; font-size:11px; text-transform:uppercase; letter-spacing:.4px;">Reporte generado por</div>
                                    <div style="margin-top:4px; color:#374151; font-size:14px; font-weight:bold;">Axel Gonzalez</div>
                                    <div style="margin-top:6px;">
                                      <a href="mailto:danigonzm@outlook.com" style="color:#2563eb; text-decoration:none;">danigonzm@outlook.com</a>
                                      <span style="color:#d1d5db;"> | </span>
                                      <a href="https://github.com/DaniAxel11" style="color:#2563eb; text-decoration:none;">GitHub</a>
                                      <span style="color:#d1d5db;"> | </span>
                                      <a href="https://www.linkedin.com/in/soy-dani-gonzalez/" style="color:#2563eb; text-decoration:none;">LinkedIn</a>
                                    </div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                buildSection("#ecfdf5", "#047857", "Impacto positivo", response.analysis().get(0)),
                buildSection("#fff7ed", "#c2410c", "Problema detectado", response.analysis().get(1)),
                buildSection("#eff6ff", "#1d4ed8", "Acciones realizadas", response.analysis().get(2)),
                buildMetricRow("Tokens prompt", response.usage().promptTokenCount()),
                buildMetricRow("Tokens respuesta", response.usage().candidatesTokenCount()),
                buildMetricRow("Tokens totales", response.usage().totalTokenCount()),
                buildMetricRow("Tiempo respuesta", response.usage().responseTimeMs() + " ms")
        );
    }

    private String buildSection(String backgroundColor, String titleColor, String title, String content) {
        return """
                <tr>
                  <td style="padding:20px 28px 0;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:%s; border-left:5px solid %s;">
                      <tr>
                        <td style="padding:16px 18px;">
                          <h2 style="margin:0 0 8px; color:%s; font-size:17px; line-height:1.3;">%s</h2>
                          <p style="margin:0; color:#374151; font-size:14px; line-height:1.55;">%s</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
                """.formatted(
                backgroundColor,
                titleColor,
                titleColor,
                escapeHtml(title),
                escapeHtml(content)
        );
    }

    private String buildMetricRow(String label, Object value) {
        return """
                <tr>
                  <td style="padding:6px 0; border-top:1px solid #f3f4f6; color:#9ca3af; font-size:12px;">%s</td>
                  <td align="right" style="padding:6px 0; border-top:1px solid #f3f4f6; color:#6b7280; font-size:12px;">%s</td>
                </tr>
                """.formatted(escapeHtml(label), escapeHtml(String.valueOf(value)));
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
