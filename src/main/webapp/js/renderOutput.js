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

function dateToRelative(source,type,val){
    var el = source;
    if ((typeof source === 'string')){
        switch (source.trim().toLowerCase()) {
            case 'none': {
                el = '';
                break;
            }
            default: {
                el = '<span style = "display:none;">' + source
                        + '</span><span >' + timeDifference(Date.now(),Date.parse(source)) + '</span>';
                break;
            }
        }
    }
    return el;
}

function fixAvailableRender(source,type,val){
    var el = source;
    if ((typeof source === 'string')){
        switch (source.trim().toLowerCase()) {
            case 'none': {
                el = '';
                break;
            }
            default: {
                el = source;
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

      var  headers = [
            { title: "Image"},
            { title: "Gate:Trigger"},
            { title: "Output"},
            { title: "Action"}
          ];
      var rows = [];
      var lastColumn=3;

      jQuery.each(data, function (imageId, imageIdObj) {
        if (imageIdObj.result.header.includes("Policy_Name")){
            headers = [
              { title: "Image"},
              { title: "Policy Name"},
              { title: "Gate:Trigger"},
              { title: "Output"},
              { title: "Action"}
            ];
            lastColumn=4;
        }
        imageIdObj.result.rows.forEach(function(row) {
           if (imageIdObj.result.header.includes("Policy_Name")){
              rows.push([
                "<div> " + row[imageIdObj.result.header.indexOf("Repo_Tag")] + '</div><div class="image-id">' + row[imageIdObj.result.header.indexOf("Image_Id")] + "</div>",
                row[imageIdObj.result.header.indexOf("Policy_Name")],
                row[imageIdObj.result.header.indexOf("Gate")] + ":" + row[imageIdObj.result.header.indexOf("Trigger")],
                row[imageIdObj.result.header.indexOf("Check_Output")],
                row[imageIdObj.result.header.indexOf("Gate_Action")],
              ]);
           } else {
              rows.push([
                  "<div>" + row[imageIdObj.result.header.indexOf("Repo_Tag")] + '</div><div class="image-id">' + row[imageIdObj.result.header.indexOf("Image_Id")] + "</div>",
                  row[imageIdObj.result.header.indexOf("Gate")] + ":" + row[imageIdObj.result.header.indexOf("Trigger")],
                  row[imageIdObj.result.header.indexOf("Check_Output")],
                  row[imageIdObj.result.header.indexOf("Gate_Action")],
              ]);
           }
        });
      });

      jQuery(document).ready(function () {
        jQuery(tableId).DataTable({
          retrieve: true,
          data: rows,
          columns: headers,
          order: [[lastColumn, 'asc']],
          columnDefs: [
            {
              targets: lastColumn,
              render: gateAction
            }
          ]
        });
      });
  }).fail(function(jqXHR, textStatus, errorThrown) {
    var alert = jQuery("<div class=\"alert alert-warning\" role=\"alert\"> Failed to generate view: cannot load JSON report artifact. </div>");
    jQuery(tableId).parent().append(alert);
    jQuery(tableId).remove();
  });
}



 var vulnerabilitiesData;
 var securityTable;

function buildSecurityTable(tableId, outputFile) {

  jQuery.getJSON(outputFile, function (tableObj) {
    vulnerabilitiesData=tableObj;

  }).done( function(){

    jQuery(document).ready(function () {
         jQuery('#fix_select').change(function(){drawSecurityTable()});
         jQuery('#severity_select').change(function(){drawSecurityTable()});
         jQuery('#severity_select_criteria').change(function(){drawSecurityTable()});

          var headersSecurityTable = [
                { title: "Image"},
                { title: "Vuln ID"},
                { title: "Severity"},
                { title: "Package"},
                { title: "Type"},
                { title: "Package Path"},
                { title: "Publish Date"},
                { title: "Fix"},
                { title: "Fix Date"},
              ];


        securityTable=jQuery(tableId).DataTable({
            retrieve: true,
            columns: headersSecurityTable,
            data: [],
            order: [[2, 'asc'], [0, 'asc']],
            columnDefs: [
                {
                    targets: 2,
                    render: severity
                },
                {
                    targets: 6,
                    render: dateToRelative
                },
                {
                    targets: 7,
                    render:fixAvailableRender
                },
                {
                    targets: 8,
                    render: dateToRelative
                }
            ]
          });
          drawSecurityTable();
        });
   }).fail(function(jqXHR, textStatus, errorThrown) {
        var alert = jQuery("<div class=\"alert alert-warning\" role=\"alert\"> Failed to generate view: cannot load JSON report artifact. </div>");

        jQuery(tableId).parent().append(alert);
        jQuery(tableId).remove();
      });;



}

function tableColFor(title,tableId) {
        return tableId.columns.findIndex(e => e.title == title);
}

function getFilteredData(totalData){
    var filteredData = {columns: [],data:[]};
    totalData.data.forEach(function(row) {
        var selectFix = jQuery('#fix_select');
        var select = jQuery('#severity_select');
        var selectCriteria = jQuery('#severity_select_criteria');
        var addRow = true

        if (selectFix){
            switch (selectFix.val()) {
                case 'Available': {
                    if (row[tableColFor("Fix Available",totalData)] == 'None')  {
                        addRow = false
                    }
                    break;
                }
                case 'None': {
                    if (row[tableColFor("Fix Available",totalData)] != 'None')  {
                        addRow = false
                    }
                    break;
                }
            }
        }



        if (select.val() ){
            switch (selectCriteria.val()) {
                case 'geq': {
                    if (severityLookup[row[tableColFor("Severity",totalData)].toLowerCase()] > severityLookup[select.val().toLowerCase()])  {
                        addRow = false
                    }
                    break;
                }
                case 'eq': {
                    if (severityLookup[row[tableColFor("Severity",totalData)].toLowerCase()] != severityLookup[select.val().toLowerCase()])  {
                        addRow = false
                    }
                    break;
                }
                case 'leq': {
                    if (severityLookup[row[tableColFor("Severity",totalData)].toLowerCase()] < severityLookup[select.val().toLowerCase()])  {
                        addRow = false
                    }
                    break;
                }
          }
      }

      if (addRow){
          filteredData.data.push(row);
      }
    });
    filteredData.columns = totalData.columns

    return filteredData;
}


function drawSecurityTable(){

  var rows = [];
  var tableData = getFilteredData(vulnerabilitiesData)
  tableData.data.forEach(function(row) {

      var vulnColumn = "";
      if (row[tableColFor("URL",tableData)].startsWith("<")) {
        // Old versions write the report adding the <a href=...
        vulnColumn = '<div style="white-space: nowrap;">' + row[tableColFor("CVE ID",tableData)] + "</div><div>" + row[tableColFor("URL",tableData)] + "</div>";
      } else {
        vulnColumn = '<a style="white-space: nowrap;" href="' + row[tableColFor("URL",tableData)] + '">' + row[tableColFor("CVE ID",tableData)] + "</a>";
      }

      rows.push([
        row[tableColFor("Tag",tableData)],
        vulnColumn,
        row[tableColFor("Severity",tableData)],
        row[tableColFor("Vulnerability Package",tableData)],
        row[tableColFor("Package Type",tableData)] || "",
        row[tableColFor("Package Path",tableData)] || "",
        row[tableColFor("Disclosure Date",tableData)] || "",
        row[tableColFor("Fix Available",tableData)],
        row[tableColFor("Solution Date",tableData)] || "",
      ]);


    });
    securityTable.clear().draw();
    securityTable.rows.add(rows); // Add new data
    securityTable.columns.adjust().draw(); // Redraw the DataTable
}


function download_csv() {
    var csv = 'sep=;';
    csv += "\n";
    var headerArray = ["Image","Vuln ID","Severity","Package","Fix","URL","type","Package Path","Publish Date","Fix Date"];
    csv += headerArray.join(';')
    csv += "\n";
    getFilteredData(vulnerabilitiesData).data.forEach(function(row) {
            csv += row.join(';');
            csv += "\n";
    });

    var hiddenElement = document.createElement('a');
    hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(csv);
    hiddenElement.target = '_blank';
    hiddenElement.download = 'vulnerabilities.csv';
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
         return Math.round(elapsed/1000) + ' seconds ago';
    }

    else if (elapsed < msPerHour) {
         return Math.round(elapsed/msPerMinute) + ' minutes ago';
    }

    else if (elapsed < msPerDay ) {
         return Math.round(elapsed/msPerHour ) + ' hours ago';
    }

    else if (elapsed < msPerWeek) {
        return Math.round(elapsed/msPerDay) + ' days ago';
    }

    else if (elapsed < msPerMonth ) {
        return Math.round(elapsed/msPerWeek ) + ' weeks ago';
    }

    else if (elapsed < msPerYear) {
        return Math.round(elapsed/msPerMonth) + ' months ago';
    }

    else {
        return Math.round(elapsed/msPerYear ) + ' years ago';
    }
}




