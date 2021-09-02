// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.report;

import java.util.Map;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.daimler.sechub.domain.scan.HTMLScanResultReportModelBuilder;
import com.daimler.sechub.sharedkernel.APIConstants;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.usecases.user.execute.UseCaseUserDownloadsJobReport;
import com.daimler.sechub.sharedkernel.usecases.user.execute.UseCaseUserStartsSynchronousScanByClient;

/**
 * The rest API for job scheduling. It shall be same obvious like
 * https://developer.github.com/v3/issues/labels/
 *
 * @author Albert Tregnaghi
 *
 */
@RestController
@EnableAutoConfiguration
@RequestMapping(APIConstants.API_PROJECT+"{projectId}") // API like https://developer.github.com/v3/issues/labels/#create-a-label
@RolesAllowed({RoleConstants.ROLE_USER, RoleConstants.ROLE_SUPERADMIN})
public class ScanReportRestController {

	@Autowired
	private HTMLScanResultReportModelBuilder htmlModelBuilder;

	@Autowired
	private DownloadScanReportService downloadReportService;

	/* @formatter:off */
	@UseCaseUserDownloadsJobReport(@Step(number=1,next= {3},name="REST API call to get JSON report",needsRestDoc=true))
	@UseCaseUserStartsSynchronousScanByClient(@Step(number=4, name="download job report and traffic light"))
	@RequestMapping(path = "/report/{jobUUID}", method = RequestMethod.GET, produces= {MediaType.APPLICATION_JSON_VALUE})
	public ScanReportResult getScanReportResultJSON(
			@PathVariable("projectId") String projectId,
			@PathVariable("jobUUID") UUID jobUUID
			) {
		/* @formatter:on */
		return fetchScanReportResult(projectId, jobUUID);

	}

	/* @formatter:off */
	@UseCaseUserDownloadsJobReport(@Step(number=2,next= {3},name="REST API call to get HTML report",needsRestDoc=true))
	@RequestMapping(path = "/report/{jobUUID}", method = RequestMethod.GET, produces= {"application/xhtml+xml", "text/html","text/html;charset=UTF-8"})
	@ResponseBody
	public ModelAndView getScanReportResultHTML(
			@PathVariable("projectId") String projectId,
			@PathVariable("jobUUID") UUID jobUUID
			) {
		/* @formatter:on */
		ScanReportResult scanResult = fetchScanReportResult(projectId, jobUUID);

		Map<String, Object> model = htmlModelBuilder.build(scanResult);
		return new ModelAndView("report/html/scanresult", model);
	}

	private ScanReportResult fetchScanReportResult(String projectId, UUID jobUUID) {
		return downloadReportService.getScanReportResult(projectId, jobUUID);
	}


}
