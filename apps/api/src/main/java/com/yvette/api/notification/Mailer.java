package com.yvette.api.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends transactional emails (plain text + a simple HTML version). Failures are logged, never thrown:
 * an email problem must not undo a paid order. Locally, mail goes to Mailpit (http://localhost:18025).
 */
@Component
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final JavaMailSender sender;
    private final String from;

    public Mailer(JavaMailSender sender, @Value("${yvette.mail.from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    /** A paragraph of text, or a call-to-action button when {@code link} is set. */
    public record Block(String text, String link) {
        public static Block text(String text) {
            return new Block(text, null);
        }

        public static Block button(String label, String link) {
            return new Block(label, link);
        }
    }

    public boolean send(String to, String subject, java.util.List<Block> blocks) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plain(blocks), html(subject, blocks));
            sender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Could not send \"{}\" email: {}", subject, e.getMessage());
            return false;
        }
    }

    private static String plain(java.util.List<Block> blocks) {
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            sb.append(b.link() == null ? b.text() : b.text() + ": " + b.link()).append("\n\n");
        }
        return sb.append("Yvette — curated for you, styled with you.\n").toString();
    }

    private static String html(String subject, java.util.List<Block> blocks) {
        StringBuilder sb = new StringBuilder("""
                <!doctype html><html><body style="margin:0;background:#fbfaf8;font-family:Helvetica,Arial,sans-serif;color:#1c1917">
                <div style="max-width:560px;margin:0 auto;padding:32px 24px">
                <p style="font-family:Georgia,serif;font-size:28px;margin:0 0 24px">Yvette</p>""");
        sb.append("<h1 style=\"font-size:20px;font-weight:600\">").append(HtmlUtils.htmlEscape(subject)).append("</h1>");
        for (Block b : blocks) {
            if (b.link() == null) {
                sb.append("<p style=\"line-height:1.6;white-space:pre-line\">")
                        .append(HtmlUtils.htmlEscape(b.text())).append("</p>");
            } else {
                sb.append("<p><a href=\"").append(HtmlUtils.htmlEscape(b.link()))
                        .append("\" style=\"display:inline-block;background:#1c1917;color:#fff;padding:12px 24px;"
                                + "text-decoration:none;font-size:14px;letter-spacing:.08em;text-transform:uppercase\">")
                        .append(HtmlUtils.htmlEscape(b.text())).append("</a></p>");
            }
        }
        return sb.append("<p style=\"color:#78716c;font-size:12px;margin-top:32px\">Yvette — curated for you, styled with you.</p>")
                .append("</div></body></html>").toString();
    }
}
