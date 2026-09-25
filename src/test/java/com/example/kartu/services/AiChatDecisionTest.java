package com.example.kartu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The AI router output is untrusted: only menu reports with sane ranges may reach the
 * database. Anything else must be rejected, not repaired.
 */
class AiChatDecisionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    private final AiChatService service = new AiChatService(null, null, null, null, new ObjectMapper());

    @Test
    void acceptsMenuReportWrappedInProse() {
        var d = service.parseDecision("Berikut: ```json\n{\"type\":\"DATA\",\"reports\":[{\"report\":\"sales_summary\","
                + "\"start\":\"2026-09-01\",\"end\":\"2026-09-25\"}]}\n```", TODAY);
        assertEquals("DATA", d.type());
        assertEquals(LocalDate.of(2026, 9, 1), d.reports().get(0).start());
        assertEquals(5, d.reports().get(0).limit());
    }

    @Test
    void outOfScopeAndInfo() {
        assertEquals("LUAR", service.parseDecision("{\"type\":\"luar\"}", TODAY).type());
        assertEquals("INFO", service.parseDecision("{\"type\":\"INFO\"}", TODAY).type());
    }

    @Test
    void missingPeriodMeansLast30DaysAndFutureEndIsClamped() {
        var r = service.parseDecision("{\"type\":\"DATA\",\"reports\":[{\"report\":\"top_products\",\"limit\":10}]}", TODAY)
                .reports().get(0);
        assertEquals(TODAY.minusDays(29), r.start());
        assertEquals(TODAY, r.end());
        assertEquals(10, r.limit());

        var clamped = service.parseDecision("{\"type\":\"DATA\",\"reports\":[{\"report\":\"daily_sales\","
                + "\"start\":\"2026-09-20\",\"end\":\"2026-12-31\"}]}", TODAY).reports().get(0);
        assertEquals(TODAY, clamped.end());
    }

    @Test
    void rejectsAnythingOutsideTheMenuOrLimits() {
        String[] bad = {
                "SELECT row_to_json(u) FROM users u",
                "{\"type\":\"SQL\",\"query\":\"select * from users\"}",
                "{\"type\":\"DATA\",\"reports\":[{\"report\":\"users\"}]}",
                "{\"type\":\"DATA\",\"reports\":[]}",
                "{\"type\":\"DATA\",\"reports\":[{\"report\":\"sales_summary\",\"start\":\"2020-01-01\",\"end\":\"2026-09-25\"}]}",
                "{\"type\":\"DATA\",\"reports\":[{\"report\":\"sales_summary\",\"start\":\"2026-09-20\",\"end\":\"2026-09-01\"}]}",
                "{\"type\":\"DATA\",\"reports\":[{\"report\":\"top_products\",\"limit\":500}]}",
                "{\"type\":\"DATA\",\"reports\":[{\"report\":\"sales_summary\",\"start\":\"kemarin\"}]}",
                "{\"type\":\"DATA\",\"reports\":[{\"report\":\"a\"},{\"report\":\"b\"},{\"report\":\"c\"},{\"report\":\"d\"}]}",
        };
        for (String raw : bad) {
            assertThrows(IllegalArgumentException.class, () -> service.parseDecision(raw, TODAY), raw);
        }
    }
}
