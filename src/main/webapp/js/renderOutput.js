const isOldTable = window.legacyEngine ?? true;

const actionLookup = {
  stop: 0,
  warn: 1,
  go: 2,
};

const severityLookup = {
  critical: 0,
  high: 1,
  medium: 2,
  low: 3,
  negligible: 4,
  unknown: 5,
};

const isSourceNoneString = (source) => {
  const isString = typeof source === "string";
  const isNone = isString && source.trim().toLowerCase() === "none";
  return isNone
}

function gateAction(source) {
  var el = `<span>${source}</span>`;
  if (
    typeof source === "string" &&
    source
      .trim()
      .toLowerCase()
      .match(/(stop|go|warn)/g)
  ) {
    const levelClassMap = {
      stop: "danger",
      go: "success",
      warn: "warning",
    };
    const labelMap = {
      stop: isOldTable ? source.toUpperCase() : 'Failed',
      go: isOldTable ? source.toUpperCase() : 'Passed',
      warn: "warning",
    }
    const policyResult = source.trim().toLowerCase();
    el = `
      <span style="display:none;">
        ${actionLookup[source.toLowerCase()]}
      </span>
      <span class="label label-${levelClassMap[policyResult]}">
        ${labelMap[policyResult]}
      </span>
    `;
  }
  return el;
}

function severity(source) {
  var el = `<span>${source}</span>"`;
  if (
    typeof source === "string" &&
    source
      .trim()
      .toLowerCase()
      .match(/(critical|high|medium|low|negligible|unknown)/g)
  ) {
    const severityClassMap = {
      critical: "danger",
      high: "warning",
      medium: "info",
      low: "success",
      negligible: "default",
      unknown: "default",
    };
    const severity = source.trim().toLowerCase();
    el = `
      <span style="display:none;">
        ${severityLookup[source.toLowerCase()]}
      </span>
      <span class="vuln label label-${severityClassMap[severity]}">
        ${source}
      </span>
    `;
  }
  return el;
}

function dateToRelative(source) {
  const isNone = isSourceNoneString(source);
  if (isNone) return "";
  return `<span style = "display:none;">${source}</span><span >${timeDifference(Date.now(),Date.parse(source))}</span>`;
}

function fixAvailableRender(source) {
  const isNone = isSourceNoneString(source);
  if (isNone) return "";
  return source;
}

const newEngineColumnsToUse = ["Repo_Tag", "Stop_Actions"];
const newEngineColumnsName = {
  Repo_Tag: "Image name",
  Stop_Actions: "Rules failed",
};

const getNewEnginePolicyEvalSummaryConf = (tableObj) => ({
  searching: false,
  paging: false,
  ordering: false,
  retrieve: true,
  data: tableObj.rows.map((item) =>
    Object.entries(item).reduce((final, [key, value]) => {
      if (newEngineColumnsToUse.includes(key)) {
        final[key] = value;
      }
      return final;
    }, {})
  ),
  columns: tableObj.header
    .filter((item) => newEngineColumnsToUse.includes(item.data))
    .map((item) => ({ ...item, title: newEngineColumnsName[item.data] })),
  columnDefs: [
    {
      targets: 1,
      render: (source) => `<span class="label label-danger">${source}</span>`,
    },
  ],
});

const getOldPolicyEvalSummaryConf = (tableObj) => ({
  retrieve: true,
  data: tableObj.rows,
  columns: tableObj.header,
  order: [[4, "asc"]],
  columnDefs: [
    {
      targets: 1,
      render: (source) => `<span class="label label-danger">${source}</span>`,
    },
    {
      targets: 2,
      render: (source) => `<span class="label label-warning">${source}</span>`,
    },
    {
      targets: 3,
      render: (source) => `<span class="label label-success">${source}</span>`,
    },
    {
      targets: 4,
      render: gateAction,
    },
  ],
});

function buildPolicyEvalSummaryTable(tableId, tableObj) {
  jQuery(document).ready(function () {
    const getConf = isOldTable ? getOldPolicyEvalSummaryConf : getNewEnginePolicyEvalSummaryConf;
    jQuery(tableId).DataTable(getConf(tableObj));
  });
}

function buildPolicyEvalTable(tableId, outputFile) {
  jQuery
    .getJSON(outputFile, function (data) {
      const hasAnyPolicyName = Object.values(data).some((obj) => obj.result.header.includes("Policy_Name"));
      var headers = [
        { title: isOldTable ? "Image" : "Image Name" },
        ...(hasAnyPolicyName ? [{ title: "Policy Name" }] : []),
        { title: isOldTable ? "Gate:Trigger" : "Rule Bundle" },
        ...(!isOldTable ? [{ title: "Rule" }] : []),
        { title: "Output" },
        { title: isOldTable ? "Action" : "Status" },
      ];
      var rows = [];
      const oldTableLastColumn = hasAnyPolicyName ? 4 : 3;
      const newTableLastColumn = hasAnyPolicyName ? 5 : 4;
      var lastColumn = isOldTable ? oldTableLastColumn : newTableLastColumn;

      jQuery.each(data, function (imageId, imageIdObj) {
        imageIdObj.result.rows.forEach(function (row) {
          const repoName = row[imageIdObj.result.header.indexOf("Repo_Tag")];
          const headerImageId = row[imageIdObj.result.header.indexOf("Image_Id")];
          const policyName = row[imageIdObj.result.header.indexOf("Policy_Name")];
          const hasPolicyName = imageIdObj.result.header.includes("Policy_Name");
          rows.push([
            `<div>${repoName}</div>${isOldTable ? `<div class="image-id">${headerImageId}</div>` : ""}`,
            ...(hasPolicyName ? [policyName] : []),
            ...(isOldTable
              ? [`${row[imageIdObj.result.header.indexOf("Gate")]}: ${row[imageIdObj.result.header.indexOf("Trigger")]}`]
              : [row[imageIdObj.result.header.indexOf("Gate")]]),
            ...(!isOldTable ? [row[imageIdObj.result.header.indexOf("Trigger")]] : []),
            row[imageIdObj.result.header.indexOf("Check_Output")],
            row[imageIdObj.result.header.indexOf("Gate_Action")],
          ]);
        });
      });

      jQuery(document).ready(function () {
        jQuery(tableId).DataTable({
          retrieve: true,
          data: rows,
          columns: headers,
          order: [[lastColumn, "asc"]],
          columnDefs: [
            {
              targets: lastColumn,
              render: gateAction,
            },
          ],
        });
      });
    })
    .fail(function (jqXHR, textStatus, errorThrown) {
      var alert = jQuery(
        '<div class="alert alert-warning" role="alert"> Failed to generate view: cannot load JSON report artifact. </div>'
      );
      jQuery(tableId).parent().append(alert);
      jQuery(tableId).remove();
    });
}

var vulnerabilitiesData;
var securityTable;

function buildSecurityTable(tableId, outputFile) {
  jQuery
    .getJSON(outputFile, function (tableObj) {
      vulnerabilitiesData = tableObj;
    })
    .done(function () {
      jQuery(document).ready(function () {
        jQuery("#fix_select").change(drawSecurityTable);
        jQuery("#severity_select").change(drawSecurityTable);
        jQuery("#severity_select_criteria").change(drawSecurityTable);

        var headersSecurityTable = [
          { title: isOldTable ? "Image" : "Image name" },
          { title: "Vuln ID" },
          { title: "Severity" },
          { title: "Package" },
          { title: "Type" },
          { title: "Package Path" },
          { title: "Publish Date" },
          { title: "Fix" },
          { title: "Fix Date" },
        ];

        securityTable = jQuery(tableId).DataTable({
          retrieve: true,
          columns: headersSecurityTable,
          data: [],
          order: [
            [2, "asc"],
            [0, "asc"],
          ],
          columnDefs: [
            {
              targets: 2,
              render: severity,
            },
            {
              targets: 6,
              render: dateToRelative,
            },
            {
              targets: 7,
              render: fixAvailableRender,
            },
            {
              targets: 8,
              render: dateToRelative,
            },
          ],
        });
        drawSecurityTable();
      });
    })
    .fail(function (jqXHR, textStatus, errorThrown) {
      var alert = jQuery(
        '<div class="alert alert-warning" role="alert"> Failed to generate view: cannot load JSON report artifact. </div>'
      );

      jQuery(tableId).parent().append(alert);
      jQuery(tableId).remove();
    });
}

function tableColFor(title, tableId) {
  return tableId.columns.findIndex((e) => e.title == title);
}

function getFilteredData(totalData) {
  var filteredData = { columns: [], data: [] };
  totalData.data.forEach(function (row) {
    var selectFix = jQuery("#fix_select");
    var select = jQuery("#severity_select");
    var selectCriteria = jQuery("#severity_select_criteria");
    var addRow = true;

    if (selectFix) {
      switch (selectFix.val()) {
        case "Available": {
          if (row[tableColFor("Fix Available", totalData)] == "None") {
            addRow = false;
          }
          break;
        }
        case "None": {
          if (row[tableColFor("Fix Available", totalData)] != "None") {
            addRow = false;
          }
          break;
        }
      }
    }

    if (select.val()) {
      switch (selectCriteria.val()) {
        case "geq": {
          if (
            severityLookup[row[tableColFor("Severity", totalData)].toLowerCase()] >
            severityLookup[select.val().toLowerCase()]
          ) {
            addRow = false;
          }
          break;
        }
        case "eq": {
          if (
            severityLookup[row[tableColFor("Severity", totalData)].toLowerCase()] !=
            severityLookup[select.val().toLowerCase()]
          ) {
            addRow = false;
          }
          break;
        }
        case "leq": {
          if (
            severityLookup[row[tableColFor("Severity", totalData)].toLowerCase()] <
            severityLookup[select.val().toLowerCase()]
          ) {
            addRow = false;
          }
          break;
        }
      }
    }

    if (addRow) {
      filteredData.data.push(row);
    }
  });
  filteredData.columns = totalData.columns;

  return filteredData;
}

function drawSecurityTable() {
  var rows = [];
  var tableData = getFilteredData(vulnerabilitiesData);
  tableData.data.forEach(function (row) {
    var vulnColumn = "";
    const cveID = row[tableColFor("CVE ID", tableData)];
    const url = row[tableColFor("URL", tableData)];
    if (row[tableColFor("URL", tableData)].startsWith("<")) {
      // Old versions write the report adding the <a href=...
      vulnColumn = `
        <div style="white-space: nowrap;">
          ${cveID}
        </div>
        <div>
         ${url}
        </dib>
      `;
    } else {
      vulnColumn = `
        <a style="white-space: nowrap;" href="${url}">
          ${cveID}
        </a>
      `;
    }

    rows.push([
      row[tableColFor("Tag", tableData)],
      vulnColumn,
      row[tableColFor("Severity", tableData)],
      row[tableColFor("Vulnerability Package", tableData)],
      row[tableColFor("Package Type", tableData)] || "",
      row[tableColFor("Package Path", tableData)] || "",
      row[tableColFor("Disclosure Date", tableData)] || "",
      row[tableColFor("Fix Available", tableData)],
      row[tableColFor("Solution Date", tableData)] || "",
    ]);
  });
  securityTable.clear().draw();
  securityTable.rows.add(rows); // Add new data
  securityTable.columns.adjust().draw(); // Redraw the DataTable
}

function download_csv() {
  var csv = "sep=;";
  csv += "\n";
  var headerArray = [
    "Image",
    "Vuln ID",
    "Severity",
    "Package",
    "Fix",
    "URL",
    "type",
    "Package Path",
    "Publish Date",
    "Fix Date",
  ];
  csv += headerArray.join(";");
  csv += "\n";
  getFilteredData(vulnerabilitiesData).data.forEach(function (row) {
    csv += row.join(";");
    csv += "\n";
  });

  var hiddenElement = document.createElement("a");
  hiddenElement.href = "data:text/csv;charset=utf-8," + encodeURI(csv);
  hiddenElement.target = "_blank";
  hiddenElement.download = "vulnerabilities.csv";
  hiddenElement.click();
}

function timeDifference(current, previous) {
  var msPerMinute = 60 * 1000;
  var msPerHour = msPerMinute * 60;
  var msPerDay = msPerHour * 24;
  var msPerWeek = msPerDay * 7;
  var msPerMonth = msPerDay * 30;
  var msPerYear = msPerDay * 365;

  var elapsed = current - previous;

  if (elapsed < msPerMinute) {
    return Math.round(elapsed / 1000) + " seconds ago";
  } else if (elapsed < msPerHour) {
    return Math.round(elapsed / msPerMinute) + " minutes ago";
  } else if (elapsed < msPerDay) {
    return Math.round(elapsed / msPerHour) + " hours ago";
  } else if (elapsed < msPerWeek) {
    return Math.round(elapsed / msPerDay) + " days ago";
  } else if (elapsed < msPerMonth) {
    return Math.round(elapsed / msPerWeek) + " weeks ago";
  } else if (elapsed < msPerYear) {
    return Math.round(elapsed / msPerMonth) + " months ago";
  } else {
    return Math.round(elapsed / msPerYear) + " years ago";
  }
}

const getFailedPolicies = (policies) => {
  return Object.values(policies).map((item) => {
    return item.result.rows.reduce((failedList, curr) => {
      const policyName = curr[curr.length -1];
      if (!failedList.include(policyName)) {
        failedList.push(policyName);
      }
      return failedList;
    }, [])
  }).flat();
};

function getSummaryRecap(id, vulnReportPath, policyReportPath) {
  Promise.all([jQuery.getJSON(vulnReportPath.replace("../", "")), jQuery.getJSON(policyReportPath.replace("../", ""))])
    .then(([vulns, policies]) => {
      jQuery(document).ready(() => {
        const failedPolicies = getFailedPolicies(policies);
        const failedPoliciesLength = failedPolicies.length;
        const vulnLength = vulns.data.length;
        jQuery(id).html(`
        <ul>
          <li>${JSON.stringify(failedPoliciesLength)} ${failedPoliciesLength > 1 ? "policies" : "policy"} failed</li>
          <li>${JSON.stringify(vulnLength)} ${vulnLength > 1 ? "vulnerabilities" : "vulnerability"} found</li>
        </ul>
      `);
      });
    })
    .catch((errors) => {
      jQuery(id).html('<div class="alert alert-warning" role="alert"> Failed to generate recap </div>');
    });
}
