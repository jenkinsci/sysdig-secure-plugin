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
  unknown: 5
}

function gateAction(source, type, val) {
  var el = '<span>' + source + '</span>';
  if ((typeof source === 'string') && source.trim().toLowerCase().match(/(stop|go|warn)/g)) {
    switch (source.trim().toLowerCase()) {
      case 'stop': {
        el = '<span style="display:none;">' + actionLookup[source.toLowerCase()]
            + '</span><span class="label label-danger">' + source.toUpperCase() + '</span>';
        break;
      }
      case 'go': {
        el = '<span style="display:none;">' + actionLookup[source.toLowerCase()]
            + '</span><span class="label label-success">' + source.toUpperCase() + '</span>';
        break;
      }
      case 'warn': {
        el = '<span style = "display:none;">' + actionLookup[source.toLowerCase()]
            + '</span><span class="label label-warning">' + source.toUpperCase() + '</span>';
        break;
      }
    }
  }
  return el;
}

function severity(source, type, val) {
  var el = '<span>' + source + '</span>';
  if ((typeof source === 'string') && source.trim().toLowerCase().match(/(critical|high|medium|low|negligible|unknown)/g)) {
    switch (source.trim().toLowerCase()) {
      case 'critical': {
        el = '<span style="display:none;">' + severityLookup[source.toLowerCase()]
            + '</span><span class="label label-danger">' + source + '</span>';
        break;
      }
      case 'high': {
        el = '<span style="display:none;">' + severityLookup[source.toLowerCase()]
            + '</span><span class="label label-warning">' + source + '</span>';
        break;
      }
      case 'medium': {
        el = '<span style = "display:none;">' + severityLookup[source.toLowerCase()]
            + '</span><span class="label label-info">' + source + '</span>';
        break;
      }
      case 'low': {
        el = '<span style="display:none;">' + severityLookup[source.toLowerCase()]
            + '</span><span class="label label-success">' + source + '</span>';
        break;
      }
      case 'negligible': {
        el = '<span style="display:none;">' + severityLookup[source.toLowerCase()]
            + '</span><span class="label label-default">' + source + '</span>';
        break;
      }
      case 'unknown': {
        el = '<span style = "display:none;">' + severityLookup[source.toLowerCase()]
            + '</span><span class="label label-default">' + source + '</span>';
        break;
      }
    }
  }
  return el;
}

function buildPolicyEvalSummaryTable(tableId, tableObj) {
  jQuery(document).ready(function () {
    jQuery(tableId).DataTable({
      retrieve: true,
      data: tableObj.rows,
      columns: tableObj.header,
      order: [[4, 'asc']],
      columnDefs: [
        {
          targets: 1,
          render: function (source, type, val) {
            return '<span class="label label-danger">' + source + '</span>';
          }
        },
        {
          targets: 2,
          render: function (source, type, val) {
            return '<span class="label label-warning">' + source + '</span>';
          }
        },
        {
          targets: 3,
          render: function (source, type, val) {
            return '<span class="label label-success">' + source + '</span>';
          }
        },
        {
          targets: 4,
          render: gateAction
        }
      ]
    });
  });
}

function buildPolicyEvalTable(tableId, outputFile) {
  jQuery.getJSON(outputFile, function (data) {
      var headers = [
        { title: "Image"},
        { title: "Gate:Trigger"},
        { title: "Output"},
        { title: "Action"}
      ];

      var rows = [];

      jQuery.each(data, function (imageId, imageIdObj) {
        imageIdObj.result.rows.forEach(function(row) {
          rows.push([
            "<div>" + row[imageIdObj.result.header.indexOf("Repo_Tag")] + '</div><div class="image-id">' + row[imageIdObj.result.header.indexOf("Image_Id")] + "</div>",
            row[imageIdObj.result.header.indexOf("Gate")] + ":" + row[imageIdObj.result.header.indexOf("Trigger")],
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
          order: [[3, 'asc']],
          columnDefs: [
            {
              targets: 3,
              render: gateAction
            }
          ]
        });
      });
  });
}


function buildSecurityTable(tableId, outputFile) {

  jQuery.getJSON(outputFile, function (tableObj) {

    var headers = [
      { title: "Image"},
      { title: "Vuln ID"},
      { title: "Severity"},
      { title: "Package"},
      { title: "Type"},
      { title: "Publish Date"},
      { title: "Fix"},
      { title: "Fix Date"},
    ];

    var rows = [];

    tableObj.data.forEach(function(row) {

      function tableColFor(title) {
        return tableObj.columns.findIndex(e => e.title == title);
      }

      var vulnColumn = "";
      if (row[tableColFor("URL")].startsWith("<")) {
        // Old versions write the report adding the <a href=...
         vulnColumn = '<div style="white-space: nowrap;">' + row[tableColFor("CVE ID")] + "</div><div>" + row[tableColFor("URL")] + "</div>";
      } else {
        vulnColumn = '<a style="white-space: nowrap;" href="' + row[tableColFor("URL")] + '">' + row[tableColFor("CVE ID")] + "</a>";
      }

      rows.push([
        row[tableColFor("Tag")],
        vulnColumn,
        row[tableColFor("Severity")],
        row[tableColFor("Vulnerability Package")],
        row[tableColFor("Package Type")] || "",
        row[tableColFor("Disclosure Date")] || "",
        row[tableColFor("Fix Available")],
        row[tableColFor("Solution Date")] || "",
      ]);
    });

    jQuery(document).ready(function () {
      jQuery(tableId).DataTable({
        retrieve: true,
        columns: headers,
        data: rows,
        order: [[2, 'asc'], [0, 'asc']],
        columnDefs: [
          {
            targets: 2,
            render: severity
          }
        ]
      });
    });
  });
}
