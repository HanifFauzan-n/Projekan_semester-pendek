package com.example.kartu;

import com.example.kartu.services.ReportService;
import com.example.kartu.services.ReportService.ReportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Needs the database from backend/.env. The report check only reads. */
@SpringBootTest
class KartuApplicationTests {

	@Autowired
	private ReportService reportService;

	@Test
	void contextLoads() {
	}

	@Test
	void everyReportOnTheAiMenuRunsAgainstTheRealDatabase() {
		LocalDate today = LocalDate.now();
		for (String name : ReportService.MENU.keySet()) {
			Map<String, Object> result = reportService.run(new ReportRequest(name, today.minusDays(60), today, 5, 50));
			assertEquals(name, result.get("laporan"));
			System.out.println(result);
		}
	}

}
