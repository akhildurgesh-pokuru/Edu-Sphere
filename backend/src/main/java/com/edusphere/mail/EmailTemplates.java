package com.edusphere.mail;

import org.springframework.web.util.HtmlUtils;

/** Builds simple, table-free-but-email-safe HTML. Every dynamic value is HTML-escaped. */
final class EmailTemplates {
    private EmailTemplates() {}

    record Content(String html, String text) {}

    static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }

    static Content render(String brand, String heading, String body, String buttonLabel, String buttonUrl, String footer) {
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html><body style=\"margin:0;padding:0;background:#f4f5f9;font-family:Segoe UI,Arial,sans-serif;color:#1f2430\">")
         .append("<div style=\"max-width:560px;margin:0 auto;padding:24px 16px\">")
         .append("<div style=\"font-size:20px;font-weight:700;color:#4f46e5;margin-bottom:16px\">").append(esc(brand)).append("</div>")
         .append("<div style=\"background:#ffffff;border-radius:12px;padding:28px;border:1px solid #e5e7ef\">")
         .append("<h1 style=\"font-size:20px;margin:0 0 12px\">").append(esc(heading)).append("</h1>")
         .append("<p style=\"font-size:15px;line-height:1.6;margin:0 0 20px;white-space:pre-line\">").append(esc(body)).append("</p>");
        if (buttonUrl != null) {
            h.append("<p style=\"margin:0 0 20px\"><a href=\"").append(esc(buttonUrl))
             .append("\" style=\"display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;font-weight:600;padding:12px 22px;border-radius:8px\">")
             .append(esc(buttonLabel)).append("</a></p>")
             .append("<p style=\"font-size:12px;color:#6b7280;margin:0;word-break:break-all\">If the button does not work, copy this link: ")
             .append(esc(buttonUrl)).append("</p>");
        }
        h.append("</div><p style=\"font-size:12px;color:#6b7280;margin:16px 4px 0\">").append(esc(footer)).append("</p></div></body></html>");
        String text = heading + "\n\n" + body + (buttonUrl != null ? "\n\n" + buttonLabel + ": " + buttonUrl : "") + "\n\n-- " + footer;
        return new Content(h.toString(), text);
    }
}
